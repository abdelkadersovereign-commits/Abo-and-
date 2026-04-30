package com.asyria.security.ui.screens

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket
import java.net.InetSocketAddress

private val android.content.Context.dataStore by preferencesDataStore(name = "threat_ledger")

enum class ThreatSeverity {
    LOW, MEDIUM, CRITICAL
}

data class ThreatEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val type: String,
    val severity: ThreatSeverity,
    val sourceIp: String,
    val description: String
)

data class ThreatScannerUiState(
    val isScanning: Boolean = true,
    val localIp: String = "127.0.0.1",
    val networkType: String = "DETECTING...",
    val trafficData: List<Float> = emptyList(),
    val activeTab: Int = 0,
    val threatLogs: List<ThreatEntry> = emptyList()
)

class ThreatScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore
    private val THREAT_KEY = stringPreferencesKey("threat_logs")
    private val gson = Gson()

    private val _uiState = MutableStateFlow(ThreatScannerUiState())
    val uiState: StateFlow<ThreatScannerUiState> = _uiState.asStateFlow()

    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private var previousRxBytes: Long = 0L
    private var previousTxBytes: Long = 0L

    init {
        detectNetwork()
        loadThreats()
        startScanner()
    }

    private fun detectNetwork() {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        
        val type = if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) "WIFI_SECURE"
        else if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) "CELLULAR_UPLINK"
        else "OFFLINE_VAULT"
        
        var ip = "127.0.0.1"
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces?.hasMoreElements() == true) {
                val iface = interfaces.nextElement()
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        ip = addr.hostAddress ?: ip
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ThreatScanner", "Failed to get IP", e)
        }

        _uiState.value = _uiState.value.copy(networkType = type, localIp = ip)
        
        previousRxBytes = TrafficStats.getTotalRxBytes()
        previousTxBytes = TrafficStats.getTotalTxBytes()
    }

    private fun loadThreats() {
        viewModelScope.launch {
            try {
                val preferences = dataStore.data.first()
                val json = preferences[THREAT_KEY] ?: ""
                if (json.isNotEmpty()) {
                    val type = object : TypeToken<List<ThreatEntry>>() {}.type
                    val savedThreats: List<ThreatEntry> = gson.fromJson(json, type)
                    _uiState.value = _uiState.value.copy(threatLogs = savedThreats)
                }
            } catch (e: Exception) {
                Log.e("ThreatScanner", "Failed to load threats", e)
            }
        }
    }

    private fun saveThreats(threats: List<ThreatEntry>) {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[THREAT_KEY] = gson.toJson(threats)
                }
            } catch (e: Exception) {
                Log.e("ThreatScanner", "Failed to save threats", e)
            }
        }
    }

    private fun startScanner() {
        viewModelScope.launch {
            val initialTraffic = List(30) { 0f }
            _uiState.value = _uiState.value.copy(trafficData = initialTraffic)

            while (_uiState.value.isScanning) {
                delay(2000)
                updateTraffic()
                
                // Real-world subnet discovery (simplified for performance)
                if (_uiState.value.networkType == "WIFI_SECURE") {
                    scanSubnet(_uiState.value.localIp)
                } else {
                    _logs.add("[INFO] ${dateFormat.format(Date())} | ACTIVE CONNECTION -> STABLE")
                    if (_logs.size > 25) _logs.removeAt(0)
                }
            }
        }
    }

    private suspend fun scanSubnet(localIp: String) = withContext(Dispatchers.IO) {
        try {
            val prefix = localIp.substringBeforeLast(".")
            // Pick a random IP in subnet to ping to prevent UI block on full subnet scan
            val target = "$prefix.${(1..254).random()}"
            val inetAddress = InetAddress.getByName(target)
            if (inetAddress.isReachable(500)) {
                withContext(Dispatchers.Main) {
                    _logs.add("[INFO] ${dateFormat.format(Date())} | NODE FOUND: $target -> ESTABLISHED")
                    if (_logs.size > 25) _logs.removeAt(0)
                }
                
                // Test for open susceptible ports (e.g. 80, 443, 22)
                val ports = listOf(80, 443, 22, 21, 3389)
                for (port in ports) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(target, port), 200)
                        socket.close()
                        // Suspicious if weird ports are open or generic finding
                        withContext(Dispatchers.Main) {
                            if (port == 22 || port == 3389 || port == 21) {
                                recordThreat("EXPOSED_PORT_$port", target)
                            } else {
                                _logs.add("[SECURE] ${dateFormat.format(Date())} | PORT $port OPEN ON $target")
                                if (_logs.size > 25) _logs.removeAt(0)
                            }
                        }
                    } catch (e: Exception) {
                        // Port closed or timeout
                    }
                }
            }
        } catch (e: Exception) {
            // Unreachable
        }
    }

    private fun updateTraffic() {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        
        val rxDiff = if (currentRx >= previousRxBytes) currentRx - previousRxBytes else 0L
        val txDiff = if (currentTx >= previousTxBytes) currentTx - previousTxBytes else 0L
        
        previousRxBytes = currentRx
        previousTxBytes = currentTx
        
        // Convert to KB/s approx (2 seconds delay)
        val totalKbps = ((rxDiff + txDiff) / 1024f) / 2f
        
        val currentTraffic = _uiState.value.trafficData.toMutableList()
        currentTraffic.removeAt(0)
        currentTraffic.add(totalKbps.coerceAtMost(500f)) // Cap for graph visibility
        _uiState.value = _uiState.value.copy(trafficData = currentTraffic)
        
        // Anomaly logic: sudden huge data spike
        if (totalKbps > 5000f) { // over 5MB/s
            recordThreat("TRAFFIC_SPIKE_DETECTED", _uiState.value.localIp)
        }
    }

    private fun recordThreat(type: String, source: String) {
        val severity = when {
            type.contains("SPIKE") -> ThreatSeverity.LOW
            type.contains("EXPOSED") -> ThreatSeverity.MEDIUM
            else -> ThreatSeverity.CRITICAL
        }
        
        val newThreat = ThreatEntry(
            timestamp = dateFormat.format(Date()),
            type = type,
            severity = severity,
            sourceIp = source,
            description = "Anomaly detected during subnet audit. Vector logged for analysis."
        )

        val updatedThreats = _uiState.value.threatLogs.toMutableList()
        updatedThreats.add(0, newThreat) // Newer first
        _uiState.value = _uiState.value.copy(threatLogs = updatedThreats)
        
        _logs.add("[DANGER] THREAT LOGGED: ${newThreat.type}")
        saveThreats(updatedThreats)
    }

    fun setTab(tab: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun purgeLogs() {
        _uiState.value = _uiState.value.copy(threatLogs = emptyList())
        saveThreats(emptyList())
        _logs.add("[SYSTEM] THREAT LEDGER PURGED BY OPERATOR")
    }

    fun setNetworkInfo(ip: String, type: String) {
        _uiState.value = _uiState.value.copy(localIp = ip, networkType = type)
    }
}

