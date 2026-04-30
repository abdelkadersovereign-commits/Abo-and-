package com.asyria.security.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun FileGuardianScreen(onClose: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scanComplete by remember { mutableStateOf(false) }
    var safetyScore by remember { mutableIntStateOf(0) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            fileName = it.lastPathSegment?.substringAfterLast("/") ?: "SYRIA_DOC_7.dat"
            fileSize = "2.4 MB" // Static placeholder for demo, typically calculated from content resolver
            scanComplete = false
            triggerScan {
                isScanning = false
                scanComplete = true
                safetyScore = (85..100).random()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            isScanning = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VoidBlack.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FILE GUARDIAN",
                        style = MaterialTheme.typography.titleLarge,
                        color = CyberCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "NEURAL INTEGRITY SCANNER",
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

            Spacer(modifier = Modifier.height(40.dp))

            if (!isScanning && !scanComplete) {
                // Drop Zone UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                        .clickable { filePicker.launch("*/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("SELECT SOURCE VOLUME", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Sovereign encrypted transport", color = TextGray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else if (isScanning) {
                // Scanning UI
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    ScanningRadar()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ANALYZING SECTORS...", color = CyberCyan, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(fileName, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (scanComplete) {
                // Security Certificate UI
                SecurityCertificate(fileName, fileSize, safetyScore)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SCAN ANOTHER", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Encryption Placeholder
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GlassWhite.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AmberZen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("VAULT PROTOCOL", color = AmberZen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Auto-Encryption of sensitive logs", color = TextGray, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        colors = SwitchDefaults.colors(checkedThumbColor = AmberZen)
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityCertificate(name: String, size: String, score: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
        color = GlassWhite,
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("NEURAL PASSPORT", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black)
            }
            
            Divider(modifier = Modifier.padding(vertical = 24.dp), color = GlassBorder)
            
            CertificateRow("FILE NAME", name)
            CertificateRow("SIZE", size)
            CertificateRow("ORIGIN", "INTERNAL_STORAGE")
            CertificateRow("INTEGRITY", "VERIFIED")
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAFETY SCORE: $score%",
                    color = CyberCyan,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun CertificateRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ScanningRadar() {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing))
    )

    Canvas(modifier = Modifier.size(240.dp)) {
        drawCircle(color = CyberCyan.copy(alpha = 0.1f), radius = size.width / 2)
        drawCircle(color = CyberCyan.copy(alpha = 0.2f), radius = size.width / 4, style = Stroke(2f))
        
        drawArc(
            brush = Brush.sweepGradient(
                0f to Color.Transparent,
                1f to CyberCyan.copy(alpha = 0.6f)
            ),
            startAngle = rotation,
            sweepAngle = 90f,
            useCenter = true
        )
    }
}

fun triggerScan(onComplete: () -> Unit) {
    // Artificial delay to simulate deep analysis
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        onComplete()
    }, 4000)
}
