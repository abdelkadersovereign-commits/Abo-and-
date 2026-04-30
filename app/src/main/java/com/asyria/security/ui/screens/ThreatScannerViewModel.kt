package com.asyria.security.ui.screens

import android.app.Application
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
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

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

    init {
        loadThreats()
        startScanner()
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
            // Initial traffic data
            val initialTraffic = List(30) { (10..60).random().toFloat() }
            _uiState.value = _uiState.value.copy(trafficData = initialTraffic)

            while (_uiState.value.isScanning) {
                delay(2000)
                updateTraffic()
                
                // Randomly detect non-lethal protocol info based on current state
                val proto = listOf("TCP", "UDP", "TLS", "QUIC", "DNS").random()
                val port = listOf(80, 443, 8080, 53, 22).random()
                _logs.add("[INFO] ${dateFormat.format(Date())} | ACTIVE CONNECTION: $proto:$port -> ESTABLISHED")
                if (_logs.size > 25) _logs.removeAt(0)

                // Detect threat with small probability
                if (Random().nextFloat() > 0.95f) {
                    recordThreat()
                }
            }
        }
    }

    private fun updateTraffic() {
        val currentTraffic = _uiState.value.trafficData.toMutableList()
        currentTraffic.removeAt(0)
        currentTraffic.add((10..150).random().toFloat())
        _uiState.value = _uiState.value.copy(trafficData = currentTraffic)
    }

    private fun recordThreat() {
        val threatTypes = listOf(
            "ARP_SPOOFING_DETECTED",
            "PORT_SCAN_ATTEMPT",
            "DNS_HIJACK_VULNERABILITY",
            "UNAUTHORIZED_NEURAL_UPLINK",
            "SQL_INJECTION_PROBE"
        )
        val severity = when (Random().nextFloat()) {
            in 0f..0.6f -> ThreatSeverity.LOW
            in 0.6f..0.9f -> ThreatSeverity.MEDIUM
            else -> ThreatSeverity.CRITICAL
        }
        
        val newThreat = ThreatEntry(
            timestamp = dateFormat.format(Date()),
            type = threatTypes.random(),
            severity = severity,
            sourceIp = "192.168.1.${(2..254).random()}",
            description = "Anomaly detected at protocol layer 4. Source vector identified."
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

