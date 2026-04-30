package com.asyria.security.ui.screens

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch // أضفنا هذا السطر لإصلاح خطأ launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.asyria.security.ui.components.ScannerOverlay

@Composable
fun LinkScannerScreen(onClose: () -> Unit) {
    var urlInput by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope() // هذا السطر الآن يعمل بفضل الـ imports
    var showQrScanner by remember { mutableStateOf(false) }

    if (showQrScanner) {
        ScannerOverlay(
            onBarcodeDetected = { payload ->
                showQrScanner = false
                urlInput = payload
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onClose = { showQrScanner = false }
        )
        return
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LINK SENTINEL",
                        style = MaterialTheme.typography.titleLarge,
                        color = CyberCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "NEURAL URL ANALYSIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.clip(CircleShape).background(GlassWhite)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // URL Input Section
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://example.com", color = TextGray) },
                label = { Text("TARGET URL", color = CyberCyan) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyberCyan
                ),
                trailingIcon = {
                    Row {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = TextGray)
                            }
                        }
                        IconButton(onClick = { showQrScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = CyberCyan)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isValidUrl(urlInput)) {
                        focusManager.clearFocus()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isScanning = true
                        scanResult = null
                        
                        // استخدام scope.launch بشكل صحيح لاستدعاء دالة suspend
                        scope.launch {
                            try {
                                val result = com.asyria.security.services.UrlReputationService.analyzeUrl(urlInput)
                                scanResult = ScanResult(
                                    verdict = if (result.isSafe) "Clear" else "Warning/Threat",
                                    description = result.details.joinToString("\n"),
                                    isSafe = result.isSafe,
                                    protocol = if (urlInput.startsWith("https")) "TLS_ENCRYPTED" else "UNENCRYPTED_TCP",
                                    statusColor = if (result.isSafe) SuccessGreen else RiskRed
                                )
                            } catch (e: Exception) {
                                // في حال فشل الاتصال بالانترنت
                                scanResult = ScanResult(
                                    verdict = "SCAN FAILED",
                                    description = "Internet connection required for neural analysis.",
                                    isSafe = false,
                                    protocol = "OFFLINE",
                                    statusColor = Color.Gray
                                )
                            } finally {
                                isScanning = false
                            }
                        }
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = urlInput.isNotBlank() && !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    disabledContainerColor = GlassWhite
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = VoidBlack, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = VoidBlack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("INITIATE SCAN", color = VoidBlack, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Results Area
            AnimatedVisibility(
                visible = scanResult != null,
                enter = fadeIn() + expandVertically()
            ) {
                scanResult?.let { result ->
                    ResultCard(result)
                }
            }
        }
    }
}

@Composable
fun ResultCard(result: ScanResult) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, result.statusColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
        color = GlassWhite,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (result.isSafe) Icons.Default.Verified else Icons.Default.Warning,
                contentDescription = null,
                tint = result.statusColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = result.verdict.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = result.statusColor,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result.description,
                color = TextGray,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PROTOCOL", color = TextGray, fontSize = 10.sp)
                Text(result.protocol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

data class ScanResult(
    val verdict: String,
    val description: String,
    val isSafe: Boolean,
    val protocol: String,
    val statusColor: Color
)

fun isValidUrl(url: String): Boolean {
    return Patterns.WEB_URL.matcher(url).matches()
}
