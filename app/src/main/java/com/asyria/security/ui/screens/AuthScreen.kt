package com.asyria.security.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asyria.security.R
import com.asyria.security.data.SessionManager
import com.asyria.security.ui.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val sessionManager = SessionManager(context)
        viewModel.initSessionManager(sessionManager)
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    SentinelTheme(mode = uiState.themeMode) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = VoidBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Background
                if (uiState.themeMode == ThemeMode.STANDARD) {
                    AnimatedBackground()
                }

                // Centered Login Form
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoginFormScreen(uiState, viewModel, haptic)
                }

                // Zen Toggle
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleTheme()
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.themeMode == ThemeMode.ZEN) Icons.Default.NightsStay else Icons.Default.WbSunny,
                            contentDescription = "Toggle Zen Mode",
                            tint = if (uiState.themeMode == ThemeMode.ZEN) AmberZen else CyberCyan
                        )
                    }
                }
                
                // Footer
                Text(
                    text = "A.SYRIA SECURITY SUITE",
                    color = TextGray.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 4.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "Background")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BgAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val brush = Brush.linearGradient(
                    colors = listOf(
                        NeuralPurple.copy(alpha = 0.1f),
                        NeonBlue.copy(alpha = 0.15f),
                        VoidBlack
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width * animValue, size.height)
                )
                drawRect(brush)
            }
            .blur(60.dp)
    )
}

@Composable
fun LoginFormScreen(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    haptic: HapticFeedback
) {
    val emailFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isLogin) {
        kotlinx.coroutines.delay(300)
        emailFocusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = VoidBlack.copy(alpha = 0.8f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (uiState.isLogin) "LOGIN TO HUB" else "CREATE NEURAL ID",
                    style = MaterialTheme.typography.titleLarge,
                    color = CyberCyan,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                CyberTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = "Operator Email",
                    icon = Icons.Default.Email,
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError,
                    modifier = Modifier.androidx.compose.ui.focus.focusRequester(emailFocusRequester)
                )

                Spacer(modifier = Modifier.height(16.dp))

                var passwordVisible by remember { mutableStateOf(false) }
                CyberTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = "Encryption Key",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePassword = { passwordVisible = !passwordVisible },
                    isError = uiState.passwordError != null,
                    errorMessage = uiState.passwordError
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.authenticate() 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = VoidBlack, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (uiState.isLogin) "INITIALIZE" else "REGISTER",
                            color = VoidBlack,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (uiState.isLogin) "Need new credentials? Create ID" else "Return to Login",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleAuthMode() 
                    }
                )
            }
        }
    }
}

@Composable
fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, color = TextGray) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = if (isError) RiskRed else CyberCyan) },
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = TextGray
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = GlassBorder,
                errorBorderColor = RiskRed,
                focusedTextColor = OffWhite,
                unfocusedTextColor = OffWhite,
                cursorColor = CyberCyan,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
            isError = isError,
            shape = RoundedCornerShape(12.dp)
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = RiskRed,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
