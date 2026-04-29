package com.asyria.security.ui.screens

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asyria.security.ui.theme.*
import kotlin.math.*
import kotlinx.coroutines.delay

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import android.util.Log
import com.asyria.security.util.ScannerUtils
import com.asyria.security.ui.screens.PrayerViewModel
import com.asyria.security.ui.screens.PrayerUiState
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Custom Modifier for Cyber Glow & Liquid Light Scale
@Composable
fun Modifier.cyberInteractive(
    onClick: () -> Unit,
    glowColor: Color = CyberCyan
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "PressScale"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 0f,
        animationSpec = tween(200),
        label = "GlowAlpha"
    )

    // Liquid Light Flow Animation
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidLight")
    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "Flow"
    )

    return this
        .scale(scale)
        .drawBehind {
            if (glowAlpha > 0f) {
                // Outer Glow
                drawRoundRect(
                    color = glowColor.copy(alpha = glowAlpha * 0.2f),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )
                
                // Liquid Light Sweep
                val sweepPath = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(24.dp.toPx())))
                }
                
                drawPath(
                    path = sweepPath,
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, glowColor.copy(alpha = glowAlpha), Color.Transparent),
                        center = center,
                    ),
                    style = Stroke(width = 2.dp.toPx()),
                    alpha = glowAlpha
                )
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    prayerViewModel: PrayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prayerState by prayerViewModel.uiState.collectAsState()
    var showAIChat by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // 0. Permissions for Camera
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            viewModel.setScannerOpen(true)
        }
    }

    // 1. Gyroscope / Accelerometer Sensor Logic
    var roll by remember { mutableStateOf(0f) } // Tilt Side-to-Side
    var pitch by remember { mutableStateOf(0f) } // Tilt Up-and-Down

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Extract orientation values and smooth them
                    roll = roll * 0.9f + (it.values[0]) * 0.1f
                    pitch = pitch * 0.9f + (it.values[1]) * 0.1f
                    
                    // Trigger haptic at limits
                    if (kotlin.math.abs(roll) > 8f || kotlin.math.abs(pitch) > 8f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // 2. Global Touch Tracking for Magnetic Pull
    var globalTouchPos by remember { mutableStateOf<Offset?>(null) }

    // 3. Zenith Protocol: Boot Sequence Logic
    var bootComplete by remember { mutableStateOf(false) }
    val bootAlpha by animateFloatAsState(
        targetValue = if (bootComplete) 1f else 0f,
        animationSpec = tween(1500, easing = EaseOutExpo),
        label = "BootAlpha"
    )
    val bootScale by animateFloatAsState(
        targetValue = if (bootComplete) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "BootScale"
    )

    LaunchedEffect(Unit) {
        // Orchestrate Start
        delay(500)
        bootComplete = true
        // Power-On Haptic Sequence
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(100)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(150)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Dynamic Atmosphere Animation
    val infiniteTransition = rememberInfiniteTransition(label = "Atmosphere")
    val shadowOffset by infiniteTransition.animateValue(
        initialValue = Offset(-4f, -4f),
        targetValue = Offset(4f, 4f),
        typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShadowShift"
    )

    // FAB Floating Animation
    val fabTranslation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FabFloat"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // 1. Neural Particle & Matrix Background Layer
        NeuralBackground(
            status = if (prayerState.isHubOpen) SystemStatus.SCANNING else uiState.status, 
            tiltX = roll, 
            tiltY = pitch, 
            bootComplete = bootComplete,
            isSpiritualMode = prayerState.isHubOpen
        )

        // 2. Atmospheric Edge Glow
        val edgePulse by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "EdgeGlow"
        )
        val statusColor = when {
            prayerState.isHubOpen -> AmberZen
            uiState.status == SystemStatus.SECURE -> CyberCyan
            uiState.status == SystemStatus.SCANNING -> NeonBlue
            else -> RiskRed
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, statusColor.copy(alpha = edgePulse * 0.2f)),
                            center = center,
                            radius = size.maxDimension
                        ),
                        blendMode = BlendMode.Screen
                    )
                }
        )

        // Nebula Gradient Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NeuralPurple.copy(alpha = 0.15f),
                            NeonBlue.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .blur(100.dp)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(modifier = Modifier.statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
                    HeaderSection(
                        score = uiState.integrityScore,
                        onSettingsClick = { viewModel.toggleSettings(true) },
                        isAiLoading = uiState.isAiLoading
                    )
                }
            },
            floatingActionButton = {
                Box(modifier = Modifier.padding(bottom = 90.dp)) {
                    CyberFloatingActionButton(
                        onClick = { showAIChat = true },
                        isActive = uiState.isAiLoading
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Central Shield
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .graphicsLayer {
                            scaleX = bootScale
                            scaleY = bootScale
                            alpha = bootAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusShield(
                            status = if (prayerState.isHubOpen) SystemStatus.SCANNING else uiState.status,
                            tiltX = roll,
                            tiltY = pitch,
                            onClick = { viewModel.runSystemAudit() },
                            accentColor = if (prayerState.isHubOpen) AmberZen else null
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = when {
                                prayerState.isHubOpen -> "SPIRITUAL SYNC"
                                uiState.status == SystemStatus.SECURE -> "SYSTEM SECURE"
                                uiState.status == SystemStatus.SCANNING -> "SCANNING PROTOCOLS..."
                                else -> "THREAT DETECTED"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4-Module Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            translationY = (1f - bootAlpha) * 100.dp.toPx()
                            alpha = bootAlpha
                        }
                ) {
                    item { 
                        ModuleCard(
                            "Network Scanner", 
                            "Radar Active",
                            shadowOffset,
                            MythicalIcon.Network,
                            tiltX = roll,
                            tiltY = pitch,
                            onClick = {
                                if (hasCameraPermission) {
                                    viewModel.setScannerOpen(true)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        ) 
                    }
                    item { 
                        ModuleCard(
                            "Threat Analysis", 
                            "Shield Check",
                            shadowOffset,
                            MythicalIcon.Threats,
                            tiltX = roll,
                            tiltY = pitch
                        ) 
                    }
                    item { 
                        ModuleCard(
                            "File Guardian", 
                            "Folder Lock",
                            shadowOffset,
                            MythicalIcon.Files,
                            tiltX = roll,
                            tiltY = pitch
                        ) 
                    }
                    item { 
                        ModuleCard(
                            "Spiritual Hub", 
                            "Adhan & Azkar",
                            shadowOffset,
                            MythicalIcon.Spiritual,
                            tiltX = roll,
                            tiltY = pitch,
                            onClick = { prayerViewModel.setHubOpen(true) }
                        ) 
                    }
                }
                
                // Sentinel Status Metrics Bar
                SentinelMetricsBar()
                
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Signature with Shimmer
        val shimmerTranslate by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
            label = "SignatureShimmer"
        )

        if (uiState.isScannerOpen) {
            ScannerOverlay(
                onBarcodeDetected = { url ->
                    viewModel.analyzeScannedLink(url)
                },
                onClose = { viewModel.setScannerOpen(false) }
            )
        }

        if (prayerState.isHubOpen) {
            SpiritualHubOverlay(
                state = prayerState,
                onClose = { prayerViewModel.setHubOpen(false) }
            )
        }

        if (uiState.showSettings) {
            SecuritySettingsPanel(
                apiKey = uiState.geminiApiKey,
                onApiKeyChange = { viewModel.updateApiKey(it) },
                onClose = { viewModel.toggleSettings(false) }
            )
        }

        if (showAIChat) {
            SentinelChatOverlay(
                chatHistory = uiState.chatHistory,
                isAiLoading = uiState.isAiLoading,
                onSendMessage = { viewModel.sendMessageToSentinel(it) },
                onClose = { showAIChat = false }
            )
        }

        uiState.auditReport?.let { report ->
            AuditReportDialog(
                report = report,
                onDismiss = { viewModel.closeAuditReport() },
                onConsultSentinel = {
                    val consultQuery = "I scanned this URL: ${report.url}. Gemini audit says it's ${report.safetyStatus} because: ${report.analysis}. Is it a threat for a Syrian inventor's environment? Explain specifically for a bug bounty perspective."
                    viewModel.sendMessageToSentinel(consultQuery)
                    viewModel.closeAuditReport()
                    showAIChat = true
                }
            )
        }

        Text(
            text = "ABOUDA.AL.SHEKH.YOSSEF",
            style = MaterialTheme.typography.labelSmall.copy(
                brush = Brush.linearGradient(
                    colors = listOf(TextGray.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.5f), TextGray.copy(alpha = 0.2f)),
                    start = Offset(shimmerTranslate - 200f, 0f),
                    end = Offset(shimmerTranslate, 0f)
                )
            ),
            fontSize = 10.sp,
            letterSpacing = 4.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun NeuralBackground(
    status: SystemStatus, 
    tiltX: Float, 
    tiltY: Float, 
    bootComplete: Boolean,
    isSpiritualMode: Boolean = false
) {
    val statusColor = when {
        isSpiritualMode -> AmberZen
        status == SystemStatus.SECURE -> CyberCyan
        status == SystemStatus.SCANNING -> NeonBlue
        else -> RiskRed
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "NeuralAesthetics")
    
    // Matrix Stream Animation
    val streamOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "MatrixStream"
    )

    // Particle Logic
    val particles = remember { List(40) { Particle() } }
    var touchPos by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { time ->
                particles.forEach { it.update(time, touchPos, tiltX, tiltY, bootComplete) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        touchPos = event.changes.firstOrNull()?.position
                        // Pass this to global touch if needed, but here it's local
                        if (event.changes.all { !it.pressed }) touchPos = null
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Parallax Shifted Background
            val offsetX = -tiltX * 10f
            val offsetY = tiltY * 10f

            // 1. Draw Cryptic Hex Streams
            val hexCodes = listOf("0x7F", "0xA3", "0xEE", "0x12", "0xFF", "0x00", "0x5C")
            val cols = (size.width / 40.dp.toPx()).toInt()
            for (i in 0 until cols) {
                val x = i * 40.dp.toPx() + offsetX * 0.5f // Slight parallax
                val speed = (i % 3 + 1) * 0.5f
                val yBase = (streamOffset * speed) % size.height
                
                for (j in 0 until 10) {
                    val y = (yBase + j * 100.dp.toPx() + offsetY * 0.5f) % size.height
                    drawContext.canvas.nativeCanvas.drawText(
                        hexCodes[(i + j) % hexCodes.size],
                        x,
                        y,
                        android.graphics.Paint().apply {
                            color = statusColor.copy(alpha = 0.05f).toArgb()
                            textSize = 24f
                            typeface = android.graphics.Typeface.MONOSPACE
                        }
                    )
                }
            }

            // 2. Draw Neural Particles
            particles.forEach { p ->
                drawCircle(
                    color = statusColor.copy(alpha = p.alpha),
                    radius = p.radius,
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }
        }
    }
}

class Particle {
    var x = (0..100).random() / 100f
    var y = (0..100).random() / 100f
    var vx = ((-10..10).random() / 10000f)
    var vy = ((-10..10).random() / 10000f)
    var radius = (2..6).random().toFloat()
    var alpha = (1..5).random() / 10f
    
    fun update(time: Long, touchPos: Offset?, tiltX: Float, tiltY: Float, bootComplete: Boolean) {
        // Zenith Protocol: Boot Swarm (Gravity towards center if not complete)
        if (!bootComplete) {
            val dx = 0.5f - x
            val dy = 0.5f - y
            x += dx * 0.1f
            y += dy * 0.1f
            return
        }

        // Natural movement + Tilt drift
        x += vx - tiltX * 0.0001f
        y += vy + tiltY * 0.0001f
        
        if (x < 0) x = 1f
        if (x > 1) x = 0f
        if (y < 0) y = 1f
        if (y > 1) y = 0f

        touchPos?.let {
            val dx = it.x / 1000f - x 
            val dy = it.y / 1000f - y
            val dist = sqrt(dx*dx + dy*dy)
            if (dist < 0.2f) {
                x += dx * 0.02f
                y += dy * 0.02f
            }
        }
    }
}

@Composable
fun SentinelMetricsBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "Metrics")
    
    val neuralLoadFloat by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutQuad), RepeatMode.Reverse),
        label = "Load"
    )
    val neuralLoad = neuralLoadFloat.toInt()
    
    val coreTempFloat by infiniteTransition.animateFloat(
        initialValue = 32f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Temp"
    )
    val coreTemp = coreTempFloat.toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(GlassWhite, RoundedCornerShape(8.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MetricItem("NEURAL LOAD", "$neuralLoad%")
        MetricItem("CORE TEMP", "$coreTemp°C")
        MetricItem("SIGNAL", "|||||")
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

enum class MythicalIcon {
    Network, Threats, Files, Spiritual
}

@Composable
fun ModuleCard(
    title: String, 
    sub: String,
    shadowOffset: Offset,
    iconType: MythicalIcon,
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    onClick: () -> Unit = {}
) {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        touchPos = event.changes.firstOrNull()?.position
                        if (event.changes.all { !it.pressed }) touchPos = null
                    }
                }
            }
            .graphicsLayer {
                // Parallax Lean
                rotationY = tiltX * 1.5f
                rotationX = -tiltY * 1.5f
                cameraDistance = 12f * density
            }
            .cyberInteractive(onClick) 
    ) {
        // Atmospheric Layer (Shadow & Stroke)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset(shadowOffset.x.dp, shadowOffset.y.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = Color.Transparent,
            shape = RoundedCornerShape(24.dp)
        ) {}

        // Main Glass Body
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Dynamic Glass Refraction (Moving Shine)
                    val shineProgress = ((tiltX + tiltY) / 10f) + 0.5f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent),
                            start = Offset(size.width * (shineProgress - 0.2f), 0f),
                            end = Offset(size.width * (shineProgress + 0.2f), size.height)
                        ),
                        blendMode = BlendMode.Overlay
                    )
                }
                .blur(2.dp),
            color = GlassWhite,
            shape = RoundedCornerShape(24.dp),
            contentColor = OffWhite
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Mythical Icon Container with Magnetic Pull
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            touchPos?.let {
                                val dx = (it.x - size.width / 2) * 0.1f
                                val dy = (it.y - size.height / 2) * 0.1f
                                translationX = dx
                                translationY = dy
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MythicalIconView(iconType)
                }

                Column {
                    Text(title, color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(sub, color = TextGray, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

@Composable
fun ScannerOverlay(
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val infiniteTransition = rememberInfiniteTransition(label = "ScannerLaser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPos"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(executor, ScannerUtils.BarcodeAnalyzer { barcode ->
                                onBarcodeDetected(barcode)
                            })
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("ScannerOverlay", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Blur overlay for atmosphere
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )

        // Custom Scanner UI Mesh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val cornerSize = 40.dp.toPx()
                    val scanSize = 250.dp.toPx()
                    val left = (size.width - scanSize) / 2
                    val top = (size.height - scanSize) / 2
                    val right = left + scanSize
                    val bottom = top + scanSize

                    // Draw focus corners
                    val cornerPath = Path().apply {
                        // Top Left
                        moveTo(left, top + cornerSize)
                        lineTo(left, top)
                        lineTo(left + cornerSize, top)

                        // Top Right
                        moveTo(right - cornerSize, top)
                        lineTo(right, top)
                        lineTo(right, top + cornerSize)

                        // Bottom Right
                        moveTo(right, bottom - cornerSize)
                        lineTo(right, bottom)
                        lineTo(right - cornerSize, bottom)

                        // Bottom Left
                        moveTo(left + cornerSize, bottom)
                        lineTo(left, bottom)
                        lineTo(left, bottom - cornerSize)
                    }
                    drawPath(cornerPath, CyberCyan, style = Stroke(strokeWidth))

                    // Laser Line
                    val laserYPos = top + (scanSize * laserY)
                    drawLine(
                        color = CyberCyan,
                        start = Offset(left + 10.dp.toPx(), laserYPos),
                        end = Offset(right - 10.dp.toPx(), laserYPos),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Draw glow behind laser
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, CyberCyan.copy(alpha = 0.3f), Color.Transparent),
                            startY = laserYPos - 40.dp.toPx(),
                            endY = laserYPos + 40.dp.toPx()
                        ),
                        topLeft = Offset(left, laserYPos - 40.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(scanSize, 80.dp.toPx()),
                        blendMode = BlendMode.Screen
                    )
                }
        )

        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .background(VoidBlack.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Text(
            text = "SCANNING NEURAL LINK...",
            color = CyberCyan,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 120.dp),
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun AuditReportDialog(
    report: AuditReport,
    onDismiss: () -> Unit,
    onConsultSentinel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(2.dp, GlassBorder, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}, // Consume clicks
            colors = CardDefaults.cardColors(containerColor = VoidBlack),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val statusColor = when (report.safetyStatus) {
                    "SECURE" -> CyberCyan
                    "MALICIOUS" -> RiskRed
                    else -> AmberZen
                }

                Icon(
                    imageVector = when (report.safetyStatus) {
                        "SECURE" -> Icons.Default.Shield
                        "MALICIOUS" -> Icons.Default.Dangerous
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "A.SYRIA AUDIT REPORT",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = report.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = report.analysis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onConsultSentinel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONSULT SENTINEL", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text("BLOCK")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("PROCEED", color = VoidBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberFloatingActionButton(
    onClick: () -> Unit,
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FAB")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "Glow"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonBlue.copy(alpha = glowAlpha), Color.Transparent),
                    center = center,
                    radius = size.width / 2
                )
            )
        }

        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = VoidBlack,
            border = BorderStroke(2.dp, NeonBlue)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            if (isActive) {
                                rotationZ = glowAlpha * 360f
                            }
                        }
                )
            }
        }
    }
}


@Composable
fun SecuritySettingsPanel(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(containerColor = VoidBlack),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "NEURAL PROTOCOLS",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Gemini 1.5 Flash API Key",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                
                TextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GlassWhite,
                        unfocusedContainerColor = GlassWhite
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GET FREE FLASH KEY", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "A.SYRIA uses Gemini's 1.5 Flash model for high-speed local security heuristics. Ensure your key is from Google AI Studio for optimal sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SAVE & INITIALIZE", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun SpiritualHubOverlay(
    state: PrayerUiState,
    onClose: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HubAlpha")
    val hubAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.8f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(VoidBlack)
                .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(TextGray.copy(alpha = 0.3f), CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "NEURAL SPIRITUAL SYNC",
                style = MaterialTheme.typography.labelLarge,
                color = AmberZen,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Prayer Countdown Circle
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                // Outer Glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AmberZen.copy(alpha = hubAlpha), Color.Transparent),
                            center = center,
                            radius = size.width / 2
                        )
                    )
                }

                CircularProgressIndicator(
                    progress = 0.7f, // Demo progress
                    modifier = Modifier.size(180.dp),
                    color = AmberZen,
                    strokeWidth = 2.dp,
                    trackColor = GlassWhite
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NEXT: ${state.nextPrayerName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                    Text(
                        text = state.countdown,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Light,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Azkar List
            Text(
                text = "DAILY FRAGMENTS",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Start),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val azkar = listOf(
                "أستغفر الله العظيم وأتوب إليه" to "Seek forgiveness and return to the light.",
                "سبحان الله وبحمده" to "Praise be to the Creator of all realms.",
                "لا إله إلا الله" to "The ultimate unity of all code and soul.",
                "اللهم صل على محمد" to "Blessings upon the guiding beacon."
            )

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(azkar.size) { index ->
                    AzkarCard(azkar[index].first, azkar[index].second)
                }
            }
        }
    }
}

@Composable
fun AzkarCard(arabic: String, english: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = arabic,
                style = MaterialTheme.typography.titleMedium,
                color = AmberZen,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = english,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun MythicalIconView(type: MythicalIcon) {
    val infiniteTransition = rememberInfiniteTransition(label = "IconAnim")
    
    when (type) {
        MythicalIcon.Network -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
                label = "RadarRotation"
            )
            Canvas(modifier = Modifier.size(32.dp)) {
                rotate(rotation) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0f), CyberCyan, CyberCyan.copy(alpha = 0f))
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                drawCircle(CyberCyan, radius = 2.dp.toPx())
                drawCircle(CyberCyan.copy(alpha = 0.2f), radius = 8.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
        }
        MythicalIcon.Threats -> {
            val shimmer by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "MetalShimmer"
            )
            Canvas(modifier = Modifier.size(32.dp)) {
                val path = Path().apply {
                    moveTo(size.width / 2, 0f)
                    lineTo(size.width, size.height * 0.2f)
                    lineTo(size.width, size.height * 0.7f)
                    quadraticBezierTo(size.width / 2, size.height, 0f, size.height * 0.7f)
                    lineTo(0f, size.height * 0.2f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        0f to NeonBlue.copy(alpha = 0.3f),
                        shimmer to CyberCyan,
                        1f to NeonBlue.copy(alpha = 0.3f)
                    )
                )
            }
        }
        MythicalIcon.Files -> {
            val lift by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutBack), RepeatMode.Reverse),
                label = "FileLift"
            )
            Canvas(modifier = Modifier.size(32.dp)) {
                drawRoundRect(
                    color = CyberCyan.copy(alpha = 0.2f),
                    size = Size(size.width, size.height * 0.8f),
                    topLeft = Offset(0f, size.height * 0.2f),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                drawRoundRect(
                    color = CyberCyan,
                    size = Size(size.width * 0.9f, size.height * 0.7f),
                    topLeft = Offset(size.width * 0.05f, size.height * 0.1f + lift.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                // Light Beam
                drawLine(
                    brush = Brush.verticalGradient(listOf(CyberCyan, Color.Transparent)),
                    start = Offset(size.width / 2, size.height * 0.5f),
                    end = Offset(size.width / 2, -10f),
                    strokeWidth = 2.dp.toPx(),
                    alpha = 0.4f
                )
            }
        }
        MythicalIcon.Spiritual -> {
            val twinkle by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "StarTwinkle"
            )
            Canvas(modifier = Modifier.size(32.dp)) {
                // Crescent
                val path = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                    val innerOval = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(size.width * 0.2f, -size.height * 0.1f, size.width * 1.2f, size.height))
                    }
                    op(this, innerOval, PathOperation.Difference)
                }
                drawPath(path, CyberCyan)
                
                // Star
                drawCircle(
                    color = AmberZen,
                    radius = 3.dp.toPx() * twinkle,
                    center = Offset(size.width * 0.7f, size.width * 0.3f)
                )
            }
        }
    }
}

@Composable
fun AIChatOverlay(
    uiState: DashboardUiState,
    onClose: () -> Unit,
    onSendMessage: (String) -> Unit,
    onApiKeyChange: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "S.E.N.T.I.N.E.L AI",
                    style = MaterialTheme.typography.titleLarge,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OffWhite)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // API Key Section
            OutlinedTextField(
                value = uiState.geminiApiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Neural Core API Key", color = TextGray) },
                placeholder = { Text("Paste Gemini API Key here...", color = TextGray.copy(alpha = 0.5f)) },
                trailingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Chat History
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .background(GlassWhite, RoundedCornerShape(16.dp))
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.chatHistory.size) { index ->
                        val chat = uiState.chatHistory[index]
                        ChatBubble(chat)
                    }
                }
                
                if (uiState.isAiLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = CyberCyan,
                        trackColor = Color.Transparent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Message Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                    placeholder = { Text("Query Sentinel...", color = TextGray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GlassWhite,
                        unfocusedContainerColor = GlassWhite,
                        cursorColor = CyberCyan,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.background(CyberCyan, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = VoidBlack)
                }
            }
            
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun HeaderSection(score: Int, onSettingsClick: () -> Unit, isAiLoading: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeaderAnim")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "OPERATOR: ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                if (isAiLoading) {
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
                        label = "NeuralPulse"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = "Sentinel Core",
                style = MaterialTheme.typography.titleLarge,
                color = OffWhite,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = GlassWhite,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$score%",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.background(GlassWhite, CircleShape).border(1.dp, GlassBorder, CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }
    }
}

@Composable
fun StatusShield(
    status: SystemStatus, 
    tiltX: Float, 
    tiltY: Float, 
    onClick: () -> Unit,
    accentColor: Color? = null
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldEngine")
    
    // Neural Color Animation
    val statusColor by animateColorAsState(
        targetValue = accentColor ?: when (status) {
            SystemStatus.SECURE -> CyberCyan
            SystemStatus.SCANNING -> NeonBlue
            SystemStatus.VULNERABLE -> RiskRed
        },
        animationSpec = tween(1000),
        label = "NeutralColor"
    )

    // Pulse Animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == SystemStatus.VULNERABLE) 1.2f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    accentColor != null -> 4000
                    status == SystemStatus.VULNERABLE -> 400
                    else -> 2000
                }, 
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Heartbeat"
    )

    // Rotation Animation (For Scanning)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (status == SystemStatus.SCANNING) 1500 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HyperRotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(240.dp)
            .graphicsLayer {
                // Parallax Lean
                rotationY = tiltX * 1.2f
                rotationX = -tiltY * 1.2f
                cameraDistance = 15f * density
            }
            .cyberInteractive(
                onClick = {
                    haptic.performHapticFeedback(
                        if (status == SystemStatus.SECURE) HapticFeedbackType.LongPress 
                        else HapticFeedbackType.TextHandleMove
                    )
                    onClick()
                },
                glowColor = statusColor
            )
    ) {
        // Background Aura
        Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(statusColor.copy(alpha = 0.2f), Color.Transparent),
                    center = center,
                    radius = size.width / 2
                ),
                blendMode = BlendMode.Screen
            )
        }

        // Inner Shield Core
        Canvas(modifier = Modifier.size(160.dp)) {
            rotate(rotation) {
                // Outer Hexagon Border
                val path = Path().apply {
                    val angle = (2 * PI / 6).toFloat()
                    val radius = size.width / 2
                    for (i in 0..5) {
                        val x = center.x + radius * cos(i * angle)
                        val y = center.y + radius * sin(i * angle)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(
                    path = path,
                    color = statusColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Neural Bits (Dots on corners)
                val bitRadius = 4.dp.toPx()
                val angle = (2 * PI / 6).toFloat()
                val radius = size.width / 2
                for (i in 0..5) {
                    val x = center.x + radius * cos(i * angle)
                    val y = center.y + radius * sin(i * angle)
                    drawCircle(statusColor, radius = bitRadius, center = Offset(x, y))
                }
            }

            // Central Ring
            drawCircle(
                statusColor.copy(alpha = 0.1f),
                radius = (size.width / 4) * pulseScale
            )
        }

        // Core Icon Overlay
        Icon(
            imageVector = when (status) {
                SystemStatus.SECURE -> Icons.Default.HealthAndSafety
                SystemStatus.SCANNING -> Icons.Default.Radar
                SystemStatus.VULNERABLE -> Icons.Default.GppBad
            },
            contentDescription = null,
            modifier = Modifier.size(64.dp).scale(pulseScale),
            tint = statusColor
        )
    }
}

