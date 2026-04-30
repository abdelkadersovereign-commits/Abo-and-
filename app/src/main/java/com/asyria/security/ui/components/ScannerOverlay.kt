package com.asyria.security.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.asyria.security.ui.theme.CyberCyan
import com.asyria.security.ui.theme.VoidBlack
import com.asyria.security.util.ScannerUtils
import java.util.concurrent.Executors

@Composable
fun ScannerOverlay(
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
                        size = Size(scanSize, 80.dp.toPx()),
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
