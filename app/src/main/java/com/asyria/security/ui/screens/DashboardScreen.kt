package com.asyria.security.ui.screens

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.widget.Toast
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asyria.security.ui.theme.*
import kotlin.math.*
import kotlinx.coroutines.delay

import androidx.compose.ui.util.lerp
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
import com.asyria.security.ui.components.ScannerOverlay

// Custom Modifier for Cyber Glow & Liquid Light Scale
@Composable
fun Modifier.cyberInteractive(
    onClick: () -> Unit,
    glowColor: Color = CyberCyan
): Modifier {
    val haptic = LocalHapticFeedback.current
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
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        )
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    prayerViewModel: PrayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prayerState by prayerViewModel.uiState.collectAsState()
    
    DashboardContent(uiState, viewModel, prayerState)
}

@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    prayerState: PrayerUiState
) {
    val layoutDirection = if (uiState.language == "AR") androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
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
    var showAIChat by remember { mutableStateOf(false) }
    var alpha by remember { mutableStateOf(1f) }

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
    
    // Permissions Request Logic
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            // Log or show feedback about limited functionality
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "Shimmer"
    )

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

    val scrollState = rememberScrollState()
    
    // Shield Animation Logic based on Scroll
    val shieldScale = lerp(1f, 0.4f, (scrollState.value.toFloat() / 500f).coerceIn(0f, 1f))
    val shieldTranslateY = lerp(0f, -280f, (scrollState.value.toFloat() / 500f).coerceIn(0f, 1f))
    val shieldAlpha = lerp(1f, 0.9f, (scrollState.value.toFloat() / 500f).coerceIn(0f, 1f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // 1. Neural Particle & Matrix Background Layer
        val atmosphereAlpha by animateFloatAsState(
            targetValue = if (uiState.isStealthMode) 0.1f else 1f,
            animationSpec = tween(1000),
            label = "StealthAlpha"
        )
        
        Box(modifier = Modifier.alpha(atmosphereAlpha)) {
            NeuralBackground(
                status = if (prayerState.isHubOpen) SystemStatus.SCANNING else uiState.status, 
                tiltX = roll, 
                tiltY = pitch, 
                bootComplete = bootComplete,
                isSpiritualMode = prayerState.isHubOpen
            )
        }

        val statusColor = when {
            prayerState.isHubOpen -> AmberZen
            uiState.status == SystemStatus.SECURE -> CyberCyan
            uiState.status == SystemStatus.SCANNING -> NeonBlue
            else -> RiskRed
        }

        // 2. Atmospheric Edge Glow
        val edgePulse by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = if (uiState.isStealthMode) 0.15f else 0.4f,
            animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "EdgeGlow"
        )

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
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(320.dp)) // Reserve space for Shield

                // 6-Module Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .height(600.dp) // Fixed height to allow scrolling in Column
                        .graphicsLayer {
                            translationY = (1f - bootAlpha) * 100.dp.toPx()
                            alpha = bootAlpha
                        }
                ) {
                    val modules = listOf(
                        Triple(if (uiState.language == "EN") "Network Scanner" else "ماصح الشبكة", if (uiState.language == "EN") "Topology Map" else "خريطة الشبكة", MythicalIcon.Network) to { viewModel.toggleNetworkScanner(true) },
                        Triple(if (uiState.language == "EN") "Link Sentinel" else "رادار الروابط", if (uiState.language == "EN") "URL Audit" else "فحص الروابط", MythicalIcon.Scanner) to { viewModel.toggleLinkScanner(true) },
                        Triple(if (uiState.language == "EN") "Media Scanner" else "ماصح الوسائط", if (uiState.language == "EN") "Vision Audit" else "فحص الرؤية", MythicalIcon.Camera) to { viewModel.toggleMediaScanner(true) },
                        Triple(if (uiState.language == "EN") "Spiritual Hub" else "الركن الروحاني", if (uiState.language == "EN") "Neural Balance" else "التوازن العصبي", MythicalIcon.Spiritual) to { viewModel.toggleSpiritualHub(true) },
                        Triple(if (uiState.language == "EN") "Sentinel AI" else "ذكاء SENTINEL", if (uiState.language == "EN") "Core Intel" else "معلومات المحرك", MythicalIcon.Sentinel) to { viewModel.toggleAiOverlay(true) },
                        Triple(if (uiState.language == "EN") "Threat Analysis" else "تحليل التهديدات", if (uiState.language == "EN") "System Integrity" else "سلامة النظام", MythicalIcon.Threats) to { viewModel.toggleThreatScanner(true) },
                        Triple(if (uiState.language == "EN") "File Guardian" else "حارس الملفات", if (uiState.language == "EN") "Vault Protocol" else "بروتوكول القبو", MythicalIcon.Files) to { viewModel.toggleFileGuardian(true) },
                        Triple(if (uiState.language == "EN") "System Settings" else "إعدادات النظام", if (uiState.language == "EN") "Config & Keys" else "المفاتيح والتكوين", MythicalIcon.Settings) to { viewModel.toggleSettings(true) }
                    )

                    items(modules.size) { index ->
                        val (info, action) = modules[index]
                        
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(bootComplete) {
                            if (bootComplete) {
                                delay(index * 100L)
                                visible = true
                            }
                        }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.8f),
                            exit = fadeOut()
                        ) {
                            ModuleCard(
                                info.first, 
                                info.second,
                                shadowOffset,
                                info.third,
                                tiltX = roll,
                                tiltY = pitch,
                                onClick = action
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Sentinel Status Metrics Bar
                AnimatedVisibility(visible = !uiState.isStealthMode) {
                    SentinelMetricsBar()
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Animated Central Shield (Positioned above everything else)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp)
                .graphicsLayer {
                    scaleX = bootScale * shieldScale
                    scaleY = bootScale * shieldScale
                    translationY = shieldTranslateY.dp.toPx()
                    alpha = bootAlpha * shieldAlpha
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
                
                // Hide text when shield is small
                if (shieldScale > 0.7f) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = when {
                            prayerState.isHubOpen -> if (uiState.language == "EN") "SPIRITUAL SYNC" else "مزامنة روحانية"
                            uiState.status == SystemStatus.SECURE -> if (uiState.language == "EN") "SYSTEM SECURE" else "النظام آمن"
                            uiState.status == SystemStatus.SCANNING -> if (uiState.language == "EN") "SCANNING..." else "جاري الفحص..."
                            else -> if (uiState.language == "EN") "THREAT DETECTED" else "تم كشف تهديد"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }
            }
        }

        // Overlays Layer
        if (uiState.isScannerOpen) {
            ScannerScreen(
                type = uiState.activeScanType,
                onClose = { viewModel.setScannerOpen(false) }
            )
        }

        if (uiState.isAiOverlayOpen) {
            SentinelAIOverlay(
                uiState = uiState,
                onSendMessage = { viewModel.sendMessageToSentinel(it) },
                onClose = { viewModel.toggleAiOverlay(false) }
            )
        }

        if (uiState.isSpiritualHubOpen) {
            SpiritualHub(onClose = { viewModel.toggleSpiritualHub(false) })
        }

        if (uiState.isNetworkScannerOpen) {
            NetworkScannerOverlay(onClose = { viewModel.toggleNetworkScanner(false) })
        }

        if (uiState.isThreatScannerOpen) {
            NetworkScannerScreen(onClose = { viewModel.toggleThreatScanner(false) })
        }

        if (uiState.isLinkScannerOpen) {
            LinkScannerScreen(onClose = { viewModel.toggleLinkScanner(false) })
        }

        if (uiState.isMediaScannerOpen) {
            MediaScannerScreen(onClose = { viewModel.toggleMediaScanner(false) })
        }

        if (uiState.showSettings) {
            SettingsScreen(
                uiState = uiState,
                viewModel = viewModel,
                onClose = { viewModel.toggleSettings(false) }
            )
        }

        if (uiState.isFileGuardianOpen) {
            FileGuardianScreen(onClose = { viewModel.toggleFileGuardian(false) })
        }

        uiState.auditReport?.let { report ->
            AuditReportDialog(
                report = report,
                onDismiss = { viewModel.closeAuditReport() },
                onConsultSentinel = {
                    val consultQuery = "I scanned this URL: ${report.url}. Gemini audit says it's ${report.safetyStatus} because: ${report.analysis}. Is it a threat for a Syrian inventor's environment? Explain specifically for a bug bounty perspective."
                    viewModel.sendMessageToSentinel(consultQuery)
                    viewModel.closeAuditReport()
                    viewModel.toggleAiOverlay(true)
                }
            )
        }

        Text(
            text = "A.SYRIA - FUTURE SECURED",
            style = MaterialTheme.typography.labelSmall.copy(
                brush = Brush.linearGradient(
                    colors = listOf(TextGray.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.5f), TextGray.copy(alpha = 0.2f)),
                    start = Offset(shimmerTranslate - 200f, 0f),
                    end = Offset(shimmerTranslate, 0f)
                )
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        )
        
        Text(
            text = "Designed by ABOUDA.AL.SHEKH.YOSSEF",
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan.copy(alpha = 0.3f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
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
                        val event = awaitPointerEvent(PointerEventPass.Initial)
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
    Network, Threats, Files, Spiritual, Settings, Sentinel, Camera, Scanner
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
    val infiniteTransition = rememberInfiniteTransition(label = "IonicGlow")
    
    val pulseIntensity by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "Pulse"
    )

    val touchPulseIntensity = if (touchPos != null) 1.2f else 1f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        touchPos = event.changes.firstOrNull()?.position
                        if (event.changes.all { !it.pressed }) touchPos = null
                    }
                }
            }
            .graphicsLayer {
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
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.3f * pulseIntensity * touchPulseIntensity),
                            NeonBlue.copy(alpha = 0.1f),
                            CyberCyan.copy(alpha = 0.3f * pulseIntensity * touchPulseIntensity)
                        )
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
                    val shineProgress = ((tiltX + tiltY) / 10f) + 0.5f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent),
                            start = Offset(size.width * (shineProgress - 0.2f), 0f),
                            end = Offset(size.width * (shineProgress + 0.2f), size.height)
                        ),
                        blendMode = BlendMode.Overlay
                    )
                },
            color = GlassWhite,
            shape = RoundedCornerShape(24.dp),
            contentColor = OffWhite
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
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
    val uriHandler = LocalUriHandler.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    
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
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
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
                    addOval(Rect(0f, 0f, size.width, size.height))
                    val innerOval = Path().apply {
                        addOval(Rect(size.width * 0.2f, -size.height * 0.1f, size.width * 1.2f, size.height))
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
        MythicalIcon.Settings -> {
            val gearRot by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "Gear"
            )
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(32.dp).graphicsLayer { rotationZ = gearRot })
            }
        }
        MythicalIcon.Sentinel -> {
            val pulse by infiniteTransition.animateFloat(
                initialValue = 0.5f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "AIPulse"
            )
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(32.dp).graphicsLayer { scaleX = pulse; scaleY = pulse })
            }
        }
        MythicalIcon.Camera -> {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
            }
        }
        MythicalIcon.Scanner -> {
            val scanAnim by infiniteTransition.animateFloat(
                initialValue = -10f, targetValue = 10f,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "ScanLine"
            )
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
                Box(Modifier.size(28.dp).offset(y = scanAnim.dp).background(CyberCyan.copy(alpha = 0.3f)).height(1.dp).fillMaxWidth())
            }
        }
    }
}

// Removed unused AIChatOverlay

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
        targetValue = if (status == SystemStatus.VULNERABLE) 1.25f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    accentColor != null -> 3000
                    status == SystemStatus.VULNERABLE -> 350
                    else -> 1200
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

