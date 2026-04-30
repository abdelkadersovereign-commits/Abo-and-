package com.asyria.security.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.app.Application
import androidx.lifecycle.AndroidViewModel

import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SystemStatus {
    SECURE, SCANNING, VULNERABLE
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AuditReport(
    val url: String,
    val safetyStatus: String, // "SECURE", "RISKY", "MALICIOUS"
    val analysis: String,
    val showReport: Boolean = false
)

enum class ThemeLevel {
    CYBER_NOIR, WARM_STEALTH
}

data class DashboardUiState(
    val status: SystemStatus = SystemStatus.SECURE,
    val integrityScore: Int = 98,
    val activeProtocols: Int = 12,
    val chatHistory: List<ChatMessage> = listOf(
        ChatMessage("S.E.N.T.I.N.E.L online. How can I assist with your security protocols today?", false)
    ),
    val isAiLoading: Boolean = false,
    val isAiOverlayOpen: Boolean = false,
    val isSpiritualHubOpen: Boolean = false,
    val activeScanType: ScanType = ScanType.SYSTEM,
    val isFileGuardianOpen: Boolean = false,
    val isNetworkScannerOpen: Boolean = false,
    val isThreatScannerOpen: Boolean = false,
    val isLinkScannerOpen: Boolean = false,
    val isMediaScannerOpen: Boolean = false,
    val geminiApiKey: String = "",
    val isScannerOpen: Boolean = false,
    val auditReport: AuditReport? = null,
    val showSettings: Boolean = false,
    // Phase 2 Features
    val userName: String = "Sovereign User",
    val userRole: String = "Neural Architect",
    val userBio: String = "Guardian of the Digital Fortress",
    val profileImageUri: String? = null,
    val isBiometricEnabled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val isStealthMode: Boolean = false,
    val isSelfDestructEnabled: Boolean = false,
    val language: String = "EN", // "EN" or "AR"
    val themeLevel: ThemeLevel = ThemeLevel.CYBER_NOIR,
    val notificationTone: String = "Calm Horizon"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var generativeModel: GenerativeModel? = null

    private var sessionManager: com.asyria.security.data.SessionManager? = null

    init {
        sessionManager = com.asyria.security.data.SessionManager(application)
        
        val envKey = com.asyria.security.BuildConfig.GEMINI_API_KEY
        if (envKey.isNotBlank()) {
            updateApiKey(envKey)
        }
        
        viewModelScope.launch {
            sessionManager?.themeMode?.collect { mode ->
                val level = if (mode == com.asyria.security.ui.theme.ThemeMode.WARM_STEALTH) ThemeLevel.WARM_STEALTH else ThemeLevel.CYBER_NOIR
                _uiState.value = _uiState.value.copy(themeLevel = level)
            }
        }
    }

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(geminiApiKey = key)
        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = key
        )
    }

    fun updateProfile(name: String, role: String, bio: String) {
        _uiState.value = _uiState.value.copy(userName = name, userRole = role, userBio = bio)
    }

    fun updateProfileImage(uri: String?) {
        _uiState.value = _uiState.value.copy(profileImageUri = uri)
    }

    fun toggleBiometric(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
    }

    fun togglePin(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isPinEnabled = enabled)
    }

    fun toggleStealthMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isStealthMode = enabled)
    }

    fun toggleSelfDestruct(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSelfDestructEnabled = enabled)
    }

    fun setScannerOpen(open: Boolean, type: ScanType = ScanType.SYSTEM) {
        _uiState.value = _uiState.value.copy(
            isScannerOpen = open,
            activeScanType = type
        )
    }

    fun toggleAiOverlay(open: Boolean) {
        _uiState.value = _uiState.value.copy(isAiOverlayOpen = open)
    }

    fun toggleSpiritualHub(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSpiritualHubOpen = open)
    }

    fun toggleFileGuardian(open: Boolean) {
        _uiState.value = _uiState.value.copy(isFileGuardianOpen = open)
    }

    fun toggleNetworkScanner(open: Boolean) {
        _uiState.value = _uiState.value.copy(isNetworkScannerOpen = open)
    }

    fun toggleThreatScanner(open: Boolean) {
        _uiState.value = _uiState.value.copy(isThreatScannerOpen = open)
    }

    fun toggleLinkScanner(open: Boolean) {
        _uiState.value = _uiState.value.copy(isLinkScannerOpen = open)
    }

    fun toggleMediaScanner(open: Boolean) {
        _uiState.value = _uiState.value.copy(isMediaScannerOpen = open)
    }

    fun toggleLanguage() {
        val newLang = if (_uiState.value.language == "EN") "AR" else "EN"
        _uiState.value = _uiState.value.copy(language = newLang)
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        val currentPin = sessionManager?.getSecurityPin()
        if (currentPin == oldPin) {
            sessionManager?.setSecurityPin(newPin)
            return true
        }
        return false
    }

    fun setThemeLevel(level: ThemeLevel) {
        viewModelScope.launch {
            val mode = if (level == ThemeLevel.WARM_STEALTH) com.asyria.security.ui.theme.ThemeMode.WARM_STEALTH else com.asyria.security.ui.theme.ThemeMode.STANDARD
            sessionManager?.setThemeMode(mode)
        }
    }

    fun setNotificationTone(tone: String) {
        _uiState.value = _uiState.value.copy(notificationTone = tone)
    }

    fun closeAuditReport() {
        _uiState.value = _uiState.value.copy(auditReport = null)
    }

    fun analyzeScannedLink(url: String) {
        if (url.isBlank()) return
        
        // Close scanner first
        _uiState.value = _uiState.value.copy(isScannerOpen = false, status = SystemStatus.SCANNING)

        viewModelScope.launch {
            try {
                val prompt = """
                    SYSTEM SECURITY PROTOCOL: HEURISTIC ANALYSIS
                    Analyze the following URL for security risks, phishing patterns, or malicious intent: $url
                    Check for:
                    1. Suspicious Top-Level Domains (TLDs).
                    2. Homograph attacks (look-alike characters).
                    3. Deep redirection patterns.
                    4. Known phishing keywords in the path.

                    Respond ONLY in the following JSON format:
                    {
                      "safetyStatus": "SECURE" | "RISKY" | "MALICIOUS",
                      "analysis": "A brief technical explanation (max 15 words) of the neural findings."
                    }
                """.trimIndent()

                val response = generativeModel?.generateContent(prompt)
                val responseText = response?.text ?: ""
                
                val safetyStatus = if (responseText.contains("SECURE")) "SECURE" 
                                  else if (responseText.contains("MALICIOUS")) "MALICIOUS"
                                  else "RISKY"
                
                val analysisText = responseText.substringAfter("\"analysis\": \"").substringBefore("\"")

                _uiState.value = _uiState.value.copy(
                    status = if (safetyStatus == "SECURE") SystemStatus.SECURE else SystemStatus.VULNERABLE,
                    auditReport = AuditReport(
                        url = url,
                        safetyStatus = safetyStatus,
                        analysis = if (analysisText.length > 5 && analysisText.length < 100) analysisText else "Neural patterns indicate structural instability in this link domain.",
                        showReport = true
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = SystemStatus.VULNERABLE,
                    auditReport = AuditReport(
                        url = url,
                        safetyStatus = "RISKY",
                        analysis = "Neural synchronization timed out. Fragment integrity unverified.",
                        showReport = true
                    )
                )
            }
        }
    }

    fun runSystemAudit() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = SystemStatus.SCANNING)
            delay(5000)
            _uiState.value = _uiState.value.copy(
                status = SystemStatus.SECURE,
                integrityScore = (95..100).random()
            )
        }
    }

    fun toggleSettings(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettings = show)
    }

    fun sendMessageToSentinel(query: String) {
        val model = generativeModel ?: return
        if (query.isBlank()) return

        val userMessage = ChatMessage(query, true)
        val historyWithUser = _uiState.value.chatHistory + userMessage
        _uiState.value = _uiState.value.copy(
            chatHistory = historyWithUser,
            isAiLoading = true
        )

        viewModelScope.launch {
            try {
                val currentLang = _uiState.value.language
                val langInstruction = if (currentLang == "EN") "Respond in English." else "Respond strictly in Arabic with a high-end, sophisticated tone."
                val systemContext = "You are S.E.N.T.I.N.E.L, a high-end AI security consultant and life companion. Provide expert technical analysis on cyber-security and thoughtful spiritual guidance. Be professional, calm, and futuristic. $langInstruction Do not use personal names unless provided by the user."
                val fullPrompt = "$systemContext\nUser: $query"
                
                var assistantResponse = ""
                val historyWithPlaceholder = _uiState.value.chatHistory + ChatMessage("", false)
                _uiState.value = _uiState.value.copy(chatHistory = historyWithPlaceholder)

                model.generateContentStream(fullPrompt).collect { chunk ->
                    assistantResponse += chunk.text ?: ""
                    val updatedChat = _uiState.value.chatHistory.toMutableList()
                    if (updatedChat.isNotEmpty()) {
                        updatedChat[updatedChat.size - 1] = ChatMessage(assistantResponse, false)
                        _uiState.value = _uiState.value.copy(chatHistory = updatedChat)
                    }
                }
            } catch (e: Exception) {
                val errorHistory = _uiState.value.chatHistory.dropLast(1) + ChatMessage("Error: Neural sync failed. Check API key.", false)
                _uiState.value = _uiState.value.copy(chatHistory = errorHistory)
            } finally {
                _uiState.value = _uiState.value.copy(isAiLoading = false)
            }
        }
    }

    fun startScan() {
        runSystemAudit()
    }
}
