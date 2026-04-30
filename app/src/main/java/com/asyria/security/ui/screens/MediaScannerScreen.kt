package com.asyria.security.ui.screens

import android.net.Uri
import android.provider.MediaStore
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun MediaScannerScreen(onClose: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanComplete by remember { mutableStateOf(false) }
    var healthScore by remember { mutableIntStateOf(0) }
    var analysisLogs = remember { mutableStateListOf<String>() }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            isVideo = context.contentResolver.getType(it)?.contains("video") == true
            scanComplete = false
            analysisLogs.clear()
            isScanning = true
            
            // Mock Deep Scan Logic
            triggerMediaScan {
                analysisLogs.add("[INFO] NEURAL DECODER INITIALIZED")
                analysisLogs.add("[INFO] SCANNING METADATA Blobs...")
                analysisLogs.add("[INFO] STEGANOGRAPHY CHECK: PASSED")
                analysisLogs.add("[WARN] HEURISTIC ANOMALY DETECTED IN FRAME 242")
                healthScore = (70..99).random()
                isScanning = false
                scanComplete = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
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
                        text = "NEURAL MEDIA SCANNER",
                        style = MaterialTheme.typography.titleLarge,
                        color = CyberCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "AI VISION POWERED INTEGRITY",
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

            Spacer(modifier = Modifier.height(32.dp))

            if (selectedMediaUri == null) {
                // Media Picker Zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                        .clickable { mediaPicker.launch(arrayOf("image/*", "video/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("IMPORT MEDIA FOR ANALYSIS", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Images or Videos up to 100MB", color = TextGray, fontSize = 12.sp)
                    }
                }
            } else {
                // Media Display Zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(2.dp, if (scanComplete) CyberCyan.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(32.dp))
                ) {
                    AsyncImage(
                        model = selectedMediaUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (isScanning) {
                        ScanningOverlay()
                    }
                    
                    if (isVideo) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp).align(Alignment.Center)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                if (scanComplete) {
                    MediaAnalysisReport(healthScore, analysisLogs)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { selectedMediaUri = null; scanComplete = false },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("SCAN NEW MEDIA", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "Scanning")
    val lineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = CyberCyan,
                start = Offset(0f, lineY * density),
                end = Offset(size.width, lineY * density),
                strokeWidth = 4f
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, CyberCyan.copy(alpha = 0.1f))))
        )
    }
}

@Composable
fun MediaAnalysisReport(score: Int, logs: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassWhite,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MEDIA HEALTH SCORE", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "$score%",
                    color = if (score > 80) SuccessGreen else RiskRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = GlassBorder)
            
            Text("AI ANALYSIS LOGS", color = TextGray, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(modifier = Modifier.height(80.dp)) {
                items(logs.size) { index ->
                    Text(
                        text = logs[index],
                        color = if (logs[index].contains("WARN")) RiskRed.copy(alpha = 0.8f) else CyberCyan.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

fun triggerMediaScan(onComplete: () -> Unit) {
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        onComplete()
    }, 3500)
}
