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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class ScanType {
    SYSTEM, THREAT, NETWORK
}

@Composable
fun ScannerScreen(
    type: ScanType = ScanType.SYSTEM,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isScanning by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentStep by remember { mutableStateOf("Initializing Core...") }
    var showReport by remember { mutableStateOf(false) }

    val scanSteps = remember(type) {
        when(type) {
            ScanType.SYSTEM -> listOf(
                "Analyzing Neural Core...",
                "Scanning System Partitions...",
                "Verifying Application Integrity...",
                "Checking Encryption Layers...",
                "Optimizing Sentinel Buffer..."
            )
            ScanType.THREAT -> listOf(
                "Scanning for Malicious Logic...",
                "Analyzing Process Behavior...",
                "Checking USB Protocols...",
                "Verifying Neural Signatures...",
                "Searching for Ghost Threads..."
            )
            ScanType.NETWORK -> listOf(
                "Mapping Network Topology...",
                "Filtering Incoming Packets...",
                "Checking for Spoofed Protocols...",
                "Scanning Active Uplinks...",
                "Verifying Firewall Integrity..."
            )
        }
    }

    LaunchedEffect(Unit) {
        for (i in 0..100) {
            progress = i / 100f
            if (i % 20 == 0 && (i/20) < scanSteps.size) {
                currentStep = scanSteps[i/20]
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            delay(50)
        }
        isScanning = false
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(500)
        showReport = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VoidBlack.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showReport) {
                // Radar / Scanning UI
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing))
                    )

                    // Outer Rings
                    repeat(3) { index ->
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f + (index * 0.1f),
                            targetValue = 1.2f + (index * 0.1f),
                            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.6f + (index * 0.2f))
                                .border(1.dp, CyberCyan.copy(alpha = 0.2f / (index + 1)), CircleShape)
                        )
                    }

                    // Rotating Scanner
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color.Transparent, CyberCyan.copy(alpha = 0.5f))
                            ),
                            startAngle = rotation,
                            sweepAngle = 90f,
                            useCenter = true
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.displayMedium,
                            color = CyberCyan,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = currentStep.uppercase(),
                    color = OffWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = CyberCyan,
                    trackColor = GlassWhite
                )
            } else {
                // Security Report
                ReportView(type = type, onClose = onClose)
            }
        }
    }
}

@Composable
fun ReportView(type: ScanType, onClose: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            color = GlassWhite,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SECURITY REPORT: CLEAN",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val message = when(type) {
                    ScanType.SYSTEM -> "All system partitions are intact. No unauthorized modifications detected in the Neural Core."
                    ScanType.THREAT -> "Malware scan completed. No active threats or suspicious process behaviors found."
                    ScanType.NETWORK -> "Network environment is secure. All uplinks are encrypted via Sentinel protocols."
                }

                Text(
                    text = message,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ACKNOWLEDGEMENT", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
