package com.asyria.security.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.lifecycle.viewModelScope
import com.asyria.security.ui.theme.ThemeMode
import com.asyria.security.data.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLogin: Boolean = true,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isAuthenticated: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.STANDARD,
    val snackbarMessage: String? = null,
    val requiresPin: Boolean = false,
    val isPinSetup: Boolean = false,
    val pendingPin: String = "",
    val pinError: String? = null
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var sessionManager: SessionManager? = null

    fun initSessionManager(manager: SessionManager) {
        sessionManager = manager
        viewModelScope.launch {
            manager.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    val savedPin = manager.getSecurityPin()
                    if (savedPin != null) {
                        _uiState.value = _uiState.value.copy(requiresPin = true)
                    } else {
                        // User logged in but no pin setup, force to setup
                        _uiState.value = _uiState.value.copy(isAuthenticated = true, isPinSetup = true)
                    }
                }
            }
        }
        viewModelScope.launch {
            manager.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    fun toggleTheme() {
        val newMode = if (_uiState.value.themeMode == ThemeMode.STANDARD) ThemeMode.ZEN else ThemeMode.STANDARD
        viewModelScope.launch {
            sessionManager?.setThemeMode(newMode)
        }
    }

    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail, emailError = null)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword, passwordError = null)
    }

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(isLogin = !_uiState.value.isLogin)
    }

    fun authenticate() {
        val state = _uiState.value
        var hasError = false

        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.value = _uiState.value.copy(emailError = "Invalid Neural ID")
            hasError = true
        }

        if (state.password.length < 6) {
            _uiState.value = _uiState.value.copy(passwordError = "Key too weak (Min 6 chars)")
            hasError = true
        }

        if (!hasError) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            viewModelScope.launch {
                // Simulation of network call for now, but enforces validation
                kotlinx.coroutines.delay(1500)
                _uiState.value = _uiState.value.copy(isLoading = false)
                sessionManager?.setLoggedIn(true)
                
                // Immediately check pin after login
                val pin = sessionManager?.getSecurityPin()
                if (pin == null) {
                    _uiState.value = _uiState.value.copy(isPinSetup = true, isAuthenticated = true)
                } else {
                    _uiState.value = _uiState.value.copy(requiresPin = true)
                }
            }
        }
    }

    fun onGoogleSignInSuccess() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            kotlinx.coroutines.delay(1000)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                snackbarMessage = "Authenticated via Neural-Google"
            )
            sessionManager?.setLoggedIn(true)
            
            val pin = sessionManager?.getSecurityPin()
            if (pin == null) {
                _uiState.value = _uiState.value.copy(isPinSetup = true, isAuthenticated = true)
            } else {
                _uiState.value = _uiState.value.copy(requiresPin = true)
            }
        }
    }

    fun onGoogleSignInFailure(error: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = "System Breach: $error")
    }

    fun verifyPin(pin: String) {
        val savedPin = sessionManager?.getSecurityPin()
        if (savedPin == pin) {
            _uiState.value = _uiState.value.copy(requiresPin = false, isAuthenticated = true)
        } else {
            _uiState.value = _uiState.value.copy(pinError = "Access Denied: Invalid PIN")
        }
    }

    fun setupPin(pin: String) {
        if (pin.length < 4) {
            _uiState.value = _uiState.value.copy(pinError = "PIN must be at least 4 digits")
            return
        }
        val isFirstStep = _uiState.value.pendingPin.isEmpty()
        if (isFirstStep) {
            _uiState.value = _uiState.value.copy(pendingPin = pin, pinError = null)
        } else {
            if (_uiState.value.pendingPin == pin) {
                sessionManager?.setSecurityPin(pin)
                _uiState.value = _uiState.value.copy(
                    isPinSetup = false, 
                    pendingPin = "", 
                    pinError = null,
                    requiresPin = false,
                    isAuthenticated = true
                )
            } else {
                _uiState.value = _uiState.value.copy(pendingPin = "", pinError = "PINs do not match. Try again.")
            }
        }
    }
    
    fun clearPinError() {
        _uiState.value = _uiState.value.copy(pinError = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
