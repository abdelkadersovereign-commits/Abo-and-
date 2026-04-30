package com.asyria.security.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        animationSpec = tween(durationMillis = 2000)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(4000)
        onTimeout()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnim")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alphaAnim.value)
                .drawBehind {
                    val brush = Brush.radialGradient(
                        colors = listOf(NeonBlue.copy(alpha = 0.3f), Color.Transparent),
                        radius = size.width / 2
                    )
                    drawRect(brush)
                }
        ) {
            NeuralBackground(status = SystemStatus.SCANNING, tiltX = 0f, tiltY = 0f, bootComplete = true)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            Text(
                text = verse,
                color = CyberCyan.copy(alpha = glowAlpha),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
