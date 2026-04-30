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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
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
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                // Background
                if (uiState.themeMode == ThemeMode.STANDARD) {
                    AnimatedBackground()
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Sophisticated Welcome Header
                    SophisticatedWelcome(uiState.themeMode)
                    
                    // Centered Login Form
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        LoginFormScreen(uiState, { onAuthSuccess() }, viewModel, haptic)
                    }
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
                BrandingFooter()
            }
        }
    }
}

@Composable
fun SophisticatedWelcome(mode: ThemeMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "WelcomeGlow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "Alpha"
    )

    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "Shimmer"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, top = 48.dp, end = 24.dp)
            .drawBehind {
                // Moving Glowing Background (Shimmer)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (mode == ThemeMode.ZEN) AmberZen.copy(alpha = 0.05f) else CyberCyan.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerTranslate, 0f),
                        end = Offset(shimmerTranslate + 500f, 500f)
                    ),
                    blendMode = BlendMode.Screen
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "لَا تَقْنَطُوا مِن رَّحْمَةِ اللَّهِ",
            style = MaterialTheme.typography.headlineMedium,
            color = if (mode == ThemeMode.ZEN) AmberZen else CyberCyan.copy(alpha = alpha),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "DO NOT DESPAIR OF THE MERCY OF ALLAH",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to the Neural Sanctuary of A.SYRIA. A fortress for the visionary mind, where sovereign technology meets spiritual resonance.",
            style = MaterialTheme.typography.bodySmall,
            color = OffWhite.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Composable
fun BoxScope.BrandingFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "A.SYRIA - FUTURE SECURED",
            color = CyberCyan.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 6.sp
        )
        Text(
            text = "BEYOND TECHNOLOGY • WITHIN SOVEREIGNTY",
            color = TextGray.copy(alpha = 0.3f),
            fontSize = 8.sp,
            letterSpacing = 2.sp
        )
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
                    start = Offset(0f, 0f),
                    end = Offset(size.width * animValue, size.height)
                )
                drawRect(brush)
            }
            .blur(60.dp)
    )
}

@Composable
fun LoginFormScreen(
    uiState: AuthUiState,
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel,
    haptic: HapticFeedback
) {
    val emailFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        kotlinx.coroutines.delay(300)
        emailFocusRequester.requestFocus()
        keyboardController?.show()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(1000)) + expandVertically(animationSpec = tween(1000)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .blur(if (uiState.isLoading) 4.dp else 0.dp),
                shape = RoundedCornerShape(24.dp),
                color = VoidBlack.copy(alpha = 0.85f)
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
                        modifier = Modifier.focusRequester(emailFocusRequester)
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
                            // Bypass directly to dashboard as requested
                            onAuthSuccess()
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
