package com.asyria.security

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.asyria.security.data.SessionManager
import com.asyria.security.ui.screens.DashboardScreen
import com.asyria.security.ui.screens.LoginScreen
import com.asyria.security.ui.screens.SplashScreen
import com.asyria.security.ui.theme.ASyriaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private val viewModel: com.asyria.security.ui.screens.DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            var showSplash by remember { mutableStateOf(true) }
            var isLoggedIn by remember { mutableStateOf(false) }
            var isAuthenticated by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                isLoggedIn = SessionManager(this@MainActivity).isLoggedIn.first()
                if (isLoggedIn && SessionManager(this@MainActivity).isBiometricEnabled.first()) {
                    authenticateWithBiometrics {
                        isAuthenticated = true
                    }
                } else {
                    isAuthenticated = true
                }
            }

            ASyriaTheme(themeMode = uiState.themeLevel.let { 
                if (it == com.asyria.security.ui.screens.ThemeLevel.WARM_STEALTH) com.asyria.security.ui.theme.ThemeMode.WARM_STEALTH 
                else com.asyria.security.ui.theme.ThemeMode.STANDARD 
            }) {
                Crossfade(targetState = showSplash) {
                    if (it) {
                        SplashScreen(onTimeout = { showSplash = false })
                    } else {
                        if (isAuthenticated) {
                            if (isLoggedIn) {
                                DashboardScreen(viewModel = viewModel)
                            } else {
                                LoginScreen()
                            }
                        } else {
                            // You can show a message or a different screen if biometric auth fails
                        }
                    }
                }
            }
        }
    }

    private fun authenticateWithBiometrics(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, 
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
