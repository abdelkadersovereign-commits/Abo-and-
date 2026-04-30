package com.asyria.security.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.asyria.security.ui.theme.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SettingsScreen(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val profilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfileImage(it.toString()) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VoidBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.language == "EN") "CORE SETTINGS" else "إعدادات النظام",
                    style = MaterialTheme.typography.titleLarge,
                    color = CyberCyan,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.clip(CircleShape).background(GlassWhite)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Section
            ProfileEditor(
                uiState = uiState,
                onImageClick = { profilePicker.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Language & Localization
            SettingsCategory(title = if (uiState.language == "EN") "LOCALIZATION" else "اللغة")
            SecurityToggle(
                title = if (uiState.language == "EN") "Arabic Interface" else "الواجهة العربية",
                subtitle = if (uiState.language == "EN") "Enable Right-to-Left Layout" else "تمكين تخطيط اليمين إلى اليسار",
                isActive = uiState.language == "AR",
                onToggle = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleLanguage() 
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance
            SettingsCategory(title = if (uiState.language == "EN") "VISUAL INTERFACE" else "المظهر البصري")
            SecurityToggle(
                title = if (uiState.language == "EN") "Warm Stealth Mode" else "وضع التخفي الدافئ",
                subtitle = if (uiState.themeLevel == ThemeLevel.CYBER_NOIR) "Mode: Cyber-Noir" else "Mode: Warm Stealth",
                isActive = uiState.themeLevel == ThemeLevel.WARM_STEALTH,
                onToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val nextLevel = if (uiState.themeLevel == ThemeLevel.CYBER_NOIR) ThemeLevel.WARM_STEALTH else ThemeLevel.CYBER_NOIR
                    viewModel.setThemeLevel(nextLevel)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // AI Intelligence
            SettingsCategory(title = if (uiState.language == "EN") "AI INTELLIGENCE" else "ذكاء SENTINEL")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GlassWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.language == "EN") "Neural API Key (Gemini)" else "مفتاح الربط العصبي",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.geminiApiKey,
                        onValueChange = { viewModel.updateApiKey(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter API Key...", color = TextGray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // System Audio
            SettingsCategory(title = if (uiState.language == "EN") "AUDIO RESONANCE" else "الرنين الصوتي")
            ToneSelector(uiState.notificationTone) { tone ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setNotificationTone(tone)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Security Hardening
            SettingsCategory(title = if (uiState.language == "EN") "SECURITY PROTOCOLS" else "بروتوكولات الأمان")
            SecurityActionItem(
                title = if (uiState.language == "EN") "Change Security PIN" else "تغيير رمز PIN",
                icon = Icons.Default.LockReset
            ) {
                // Future Implementation for Keypad
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "A.SYRIA OS v4.2.0-STABLE",
                    color = TextGray.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Designed by ABOUDA.AL.SHEKH.YOSSEF",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ProfileEditor(uiState: DashboardUiState, onImageClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ProfileGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(GlassWhite)
                .border(2.dp, CyberCyan.copy(alpha = glowAlpha), CircleShape)
                .clickable { onImageClick() },
            contentAlignment = Alignment.Center
        ) {
            if (uiState.profileImageUri != null) {
                AsyncImage(
                    model = uiState.profileImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(48.dp))
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CyberCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(uiState.userName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(uiState.userRole, color = CyberCyan.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        color = TextGray,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp),
        letterSpacing = 2.sp
    )
}

@Composable
fun SecurityToggle(title: String, subtitle: String, isActive: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subtitle, color = TextGray, fontSize = 11.sp)
            }
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
            )
        }
    }
}

@Composable
fun ToneSelector(currentTone: String, onSelect: (String) -> Unit) {
    val tones = listOf("CALM_CHIME", "NEURAL_PULSE", "ZEN_RESONANCE", "STEALTH_CLICK")
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            tones.forEach { tone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(tone) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTone == tone,
                        onClick = { onSelect(tone) },
                        colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tone.replace("_", " "),
                        color = if (currentTone == tone) Color.White else TextGray,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityActionItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = GlassWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AmberZen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
        }
    }
}
