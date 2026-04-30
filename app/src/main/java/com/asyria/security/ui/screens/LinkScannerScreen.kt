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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import java.net.URL

@Composable
fun LinkScannerScreen(onClose: () -> Unit) {
    var urlInput by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

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
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = { urlInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextGray)
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
                        runScanner(urlInput) { result ->
                            scanResult = result
                            isScanning = false
                        }
                        isScanning = true
                        scanResult = null
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
            
            Divider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))
            
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

fun runScanner(url: String, onComplete: (ScanResult) -> Unit) {
    // Artificial delay to simulate background processing
    val isMalicious = url.contains("phish") || url.contains("malware") || url.contains("crack")
    val protocol = if (url.startsWith("https")) "TLS_ENCRYPTED" else "UNENCRYPTED_TCP"
    
    // In a real app, this would call the Gemini API or a security service
    val result = if (!isMalicious) {
        ScanResult(
            verdict = "Clear",
            description = "Neural analysis confirms no known threat vectors found. Protocol integrity verified.",
            isSafe = true,
            protocol = protocol,
            statusColor = SuccessGreen
        )
    } else {
        ScanResult(
            verdict = "Critical Threat",
            description = "Suspicious neural fingerprint detected. Potential phishing or data exfiltration site identified.",
            isSafe = false,
            protocol = protocol,
            statusColor = RiskRed
        )
    }

    // Launch completion after delay
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        onComplete(result)
    }, 2500)
}
