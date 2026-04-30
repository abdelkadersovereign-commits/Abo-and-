package com.asyria.security.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = VoidBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                val (bg, header, form, toggle, footer) = createRefs()

                // Background
                if (uiState.themeMode == ThemeMode.STANDARD) {
                    Box(modifier = Modifier.constrainAs(bg) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }) {
                        AnimatedBackground()
                    }
                }

                Box(modifier = Modifier.constrainAs(header) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }) {
                    SophisticatedWelcome(uiState.themeMode)
                }

                Box(
                    modifier = Modifier.constrainAs(form) {
                        top.linkTo(header.bottom)
                        bottom.linkTo(footer.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.requiresPin || uiState.isPinSetup) {
                        PinEntryScreen(uiState = uiState, viewModel = viewModel, haptic = haptic)
                    } else {
                        LoginFormScreen(uiState, { onAuthSuccess() }, viewModel, haptic)
                    }
                }

                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .constrainAs(toggle) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                        }
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
                
                Box(modifier = Modifier.constrainAs(footer) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }) {
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Designed by ABOUDA.AL.SHEKH.YOSSEF",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan.copy(alpha = 0.3f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
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

@Composable
fun PinEntryScreen(uiState: AuthUiState, viewModel: AuthViewModel, haptic: HapticFeedback) {
    var pin by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        val title = if (uiState.requiresPin) "ENTER SECURITY PIN" 
            else if (uiState.pendingPin.isEmpty()) "CREATE SECURITY PIN" 
            else "CONFIRM SECURITY PIN"
            
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = CyberCyan,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (i < pin.length) CyberCyan else GlassBorder)
                )
            }
        }

        if (uiState.pinError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(uiState.pinError, color = RiskRed, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        val keys = listOf("1","2","3","4","5","6","7","8","9","C","0","OK")
        var colIndex = 0
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (row in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    for (col in 0 until 3) {
                        val key = keys[colIndex++]
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GlassWhite)
                                .border(1.dp, GlassBorder, CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.clearPinError()
                                    if (key == "C") {
                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    } else if (key == "OK") {
                                        if (uiState.requiresPin) viewModel.verifyPin(pin)
                                        else viewModel.setupPin(pin)
                                        pin = ""
                                    } else {
                                        if (pin.length < 4) pin += key
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = key, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
