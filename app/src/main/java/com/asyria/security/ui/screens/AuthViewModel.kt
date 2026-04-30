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
    val snackbarMessage: String? = null
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
                    _uiState.value = _uiState.value.copy(isAuthenticated = true)
                }
            }
        }
    }

    fun toggleTheme() {
        val newMode = if (_uiState.value.themeMode == ThemeMode.STANDARD) ThemeMode.ZEN else ThemeMode.STANDARD
        _uiState.value = _uiState.value.copy(themeMode = newMode)
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
                _uiState.value = _uiState.value.copy(isLoading = false, isAuthenticated = true)
                sessionManager?.setLoggedIn(true)
            }
        }
    }

    fun onGoogleSignInSuccess() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            kotlinx.coroutines.delay(1000)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticated = true,
                snackbarMessage = "Authenticated via Neural-Google"
            )
            sessionManager?.setLoggedIn(true)
        }
    }

    fun onGoogleSignInFailure(error: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = "System Breach: $error")
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
