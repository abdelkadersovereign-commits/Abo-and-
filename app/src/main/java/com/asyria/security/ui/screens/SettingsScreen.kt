package com.asyria.security.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

import androidx.compose.foundation.lazy.grid.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.coil-kt.coil-compose.AsyncImage

@Composable
fun SettingsScreen(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfileImage(it.toString()) }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500, easing = EaseOutQuart)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(500, easing = EaseInQuart)) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VoidBlack.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isVisible = false
                        onClose()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CyberCyan)
                    }
                    Text(
                        text = "SYSTEM CONFIG",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Box(Modifier.size(48.dp)) // Placeholder for symmetry
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Section
                ProfileSection(
                    name = uiState.userName,
                    role = uiState.userRole,
                    bio = uiState.userBio,
                    imageUri = uiState.profileImageUri,
                    onEditClick = { showProfileEdit = true },
                    onAvatarClick = { imagePickerLauncher.launch("image/*") }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Security Controls
                SettingsCategory(title = "SECURITY PROTOCOLS")
                
                SecurityToggle(
                    title = "Fingerprint Unlock",
                    subtitle = "Biometric authentication for initialization",
                    isActive = uiState.isBiometricEnabled,
                    onToggle = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!uiState.isBiometricEnabled) {
                            tryAuth(context as FragmentActivity) { success ->
                                if (success) viewModel.toggleBiometric(true)
                            }
                        } else {
                            viewModel.toggleBiometric(false)
                        }
                    }
                )

                SecurityToggle(
                    title = "PIN Code Protection",
                    subtitle = "Fallback encryption sequence",
                    isActive = uiState.isPinEnabled,
                    onToggle = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!uiState.isPinEnabled) {
                            showPinDialog = true
                        } else {
                            viewModel.togglePin(false)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Appearance
                SettingsCategory(title = "VISUAL INTERFACE")
                SecurityToggle(
                    title = "Interface Theme",
                    subtitle = if (uiState.themeLevel == ThemeLevel.CYBER_NOIR) "Mode: Cyber-Noir" else "Mode: Warm Stealth",
                    isActive = uiState.themeLevel == ThemeLevel.WARM_STEALTH,
                    onToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nextLevel = if (uiState.themeLevel == ThemeLevel.CYBER_NOIR) ThemeLevel.WARM_STEALTH else ThemeLevel.CYBER_NOIR
                        viewModel.setThemeLevel(nextLevel)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Audio
                SettingsCategory(title = "AUDIO FEEDBACK")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassWhite.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .border(1.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(text = "System Alert Tone", color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val tones = listOf("CALM_CHIME", "NEURAL_PULSE", "ZEN_RESONANCE", "STEALTH_CLICK")
                    tones.forEach { tone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setNotificationTone(tone) 
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.notificationTone == tone,
                                onClick = { viewModel.setNotificationTone(tone) },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan, unselectedColor = TextGray)
                            )
                            Text(text = tone.replace("_", " "), color = if (uiState.notificationTone == tone) OffWhite else TextGray, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // System Controls
                SettingsCategory(title = "SYSTEM OVERRIDE")

                SecurityToggle(
                    title = "System Language",
                    subtitle = if (uiState.language == "EN") "Current: English" else "الحالية: العربية",
                    isActive = uiState.language == "AR",
                    onToggle = { viewModel.toggleLanguage() }
                )

                SecurityToggle(
                    title = "Stealth Mode",
                    subtitle = "Minimize visual footprint and logs",
                    isActive = uiState.isStealthMode,
                    onToggle = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleStealthMode(!uiState.isStealthMode) 
                    }
                )

                SecurityToggle(
                    title = "Data Self-Destruct",
                    subtitle = "Protocol Core-Zero: Clean wipe on 3 fails",
                    isActive = uiState.isSelfDestructEnabled,
                    onToggle = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSelfDestruct(!uiState.isSelfDestructEnabled) 
                    },
                    accentColor = RiskRed
                )

                Spacer(modifier = Modifier.height(40.dp))

                // API Section
                SettingsCategory(title = "NEURAL CORE")
                OutlinedTextField(
                    value = uiState.geminiApiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    label = { Text("Gemini API Key", color = TextGray) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "A.SYRIA SECURITY SUITE v2.5.0",
                    color = TextGray.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    if (showPinDialog) {
        PinEntryDialog(
            onConfirm = { 
                viewModel.togglePin(true)
                showPinDialog = false 
            },
            onDismiss = { showPinDialog = false }
        )
    }

    if (showProfileEdit) {
        ProfileEditDialog(
            initialName = uiState.userName,
            initialRole = uiState.userRole,
            initialBio = uiState.userBio,
            onSave = { name, role, bio ->
                viewModel.updateProfile(name, role, bio)
                showProfileEdit = false
            },
            onDismiss = { showProfileEdit = false }
        )
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        color = TextGray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun SecurityToggle(
    title: String,
    subtitle: String,
    isActive: Boolean,
    onToggle: () -> Unit,
    accentColor: Color = CyberCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(GlassWhite.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = subtitle, color = TextGray, fontSize = 12.sp)
        }
        
        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = VoidBlack,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = GlassWhite
            )
        )
    }
}

@Composable
fun PinEntryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.9f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clickable(enabled = false) {}
                    .border(2.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
                color = VoidBlack,
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SET SECURITY PIN",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // PIN Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(4) { index ->
                            val filled = index < pin.length
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (filled) CyberCyan else GlassWhite)
                                    .border(1.dp, if (filled) CyberCyan else TextGray, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Keypad
                    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(numbers.size) { index ->
                            val num = numbers[index]
                            if (num.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(GlassWhite)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (num == "DEL") {
                                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            } else if (pin.length < 4) {
                                                pin += num
                                                if (pin.length == 4) {
                                                    // Auto confirm for demo
                                                    onConfirm()
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = num,
                                        color = if (num == "DEL") RiskRed else OffWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(64.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    name: String, 
    role: String, 
    bio: String,
    imageUri: String?,
    onEditClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, CyberCyan, CircleShape)
                .background(VoidBlack)
                .clickable { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAvatarClick() 
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = CyberCyan.copy(alpha = 0.5f), modifier = Modifier.size(50.dp))
            }
            
            // Neon Circle Glow
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.3f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = name.uppercase(),
            color = OffWhite,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = role,
            color = CyberCyan,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = bio,
            color = TextGray,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEditClick() 
            },
            modifier = Modifier.height(40.dp),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("EDIT PROTOCOL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileEditDialog(
    initialName: String,
    initialRole: String,
    initialBio: String,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var role by remember { mutableStateOf(initialRole) }
    var bio by remember { mutableStateOf(initialBio) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .border(2.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            color = VoidBlack,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("EDIT OPERATOR PROFILE", color = CyberCyan, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Identity Name", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Designation", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Protocol Objective", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onSave(name, role, bio) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("UPDATE PROTOCOL", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun tryAuth(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(true)
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Authorization")
        .setSubtitle("Confirm neural signature")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}
