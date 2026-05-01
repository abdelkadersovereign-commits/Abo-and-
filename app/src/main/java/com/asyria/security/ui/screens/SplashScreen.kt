package com.asyria.security.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.R
import com.asyria.security.ui.theme.CyberCyan
import com.asyria.security.ui.theme.NeonBlue
import com.asyria.security.ui.theme.VoidBlack
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val verses = listOf(
        stringResource(id = R.string.quran_verse_1),
        stringResource(id = R.string.quran_verse_2),
        stringResource(id = R.string.quran_verse_3),
        stringResource(id = R.string.quran_verse_4)
    )
    val verse = remember { verses.random() }

    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 2000),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(4500)
        onTimeout()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnim")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val cornerPulse by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corner_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentAlignment = Alignment.Center
    ) {
        // Radial background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnim.value)
                .drawBehind {
                    val brush = Brush.radialGradient(
                        colors = listOf(NeonBlue.copy(alpha = 0.25f), Color.Transparent),
                        radius = size.width * 0.65f
                    )
                    drawRect(brush)
                }
        ) {
            NeuralBackground(status = SystemStatus.SCANNING, tiltX = 0f, tiltY = 0f, bootComplete = true)
        }

        // Glowing frame card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .padding(horizontal = 28.dp)
                .drawBehind {
                    // Outer soft glow layers
                    for (layer in 1..6) {
                        val layerAlpha = glowAlpha * (0.08f / layer)
                        val expand = (layer * 6).dp.toPx()
                        drawRoundRect(
                            color = CyberCyan.copy(alpha = layerAlpha),
                            topLeft = androidx.compose.ui.geometry.Offset(-expand, -expand),
                            size = androidx.compose.ui.geometry.Size(size.width + expand * 2, size.height + expand * 2),
                            cornerRadius = CornerRadius((cornerPulse + layer * 2).dp.toPx()),
                            style = Stroke(width = (layer * 3).dp.toPx())
                        )
                    }
                }
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = glowAlpha),
                            NeonBlue.copy(alpha = glowAlpha * 0.6f),
                            CyberCyan.copy(alpha = glowAlpha * 0.4f),
                            NeonBlue.copy(alpha = glowAlpha),
                        )
                    ),
                    shape = RoundedCornerShape(cornerPulse.dp)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.12f),
                            CyberCyan.copy(alpha = 0.04f),
                            NeonBlue.copy(alpha = 0.08f),
                        )
                    ),
                    shape = RoundedCornerShape(cornerPulse.dp)
                )
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            // Top decoration
            Text(
                text = "﷽",
                color = CyberCyan.copy(alpha = glowAlpha * 0.9f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            // Separator line
            Box(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .fillMaxWidth(0.6f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, CyberCyan.copy(alpha = glowAlpha * 0.7f), Color.Transparent)
                        )
                    )
            )

            // Quran verse
            Text(
                text = verse,
                color = CyberCyan.copy(alpha = 0.85f + glowAlpha * 0.15f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp,
                letterSpacing = 0.5.sp
            )

            // Bottom separator
            Box(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .fillMaxWidth(0.4f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, NeonBlue.copy(alpha = glowAlpha * 0.5f), Color.Transparent)
                        )
                    )
            )

            // Bottom corner decorations
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("◈", color = CyberCyan.copy(alpha = glowAlpha * 0.6f), fontSize = 10.sp)
                Spacer(Modifier.width(8.dp))
                Text("◆", color = NeonBlue.copy(alpha = glowAlpha * 0.5f), fontSize = 8.sp)
                Spacer(Modifier.width(8.dp))
                Text("◈", color = CyberCyan.copy(alpha = glowAlpha * 0.6f), fontSize = 10.sp)
            }
        }
    }
}
