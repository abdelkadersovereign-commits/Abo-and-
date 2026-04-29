package com.asyria.security.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val verses = listOf(
        "\"وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ\" (57:4)",
        "\"إِنَّ مَعَ الْعُسْرِ يُسْرًا\" (94:6)",
        "\"يَعْلَمُ مَا فِي الصُّدُورِ\" (67:13)",
        "\"وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ\" (7:156)",
        "\"فَاصْبِرْ إِنَّ وَعْدَ اللَّهِ حَقٌّ\" (30:60)",
        "\"أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ\" (13:28)"
    )
    val randomVerse = remember { verses.random() }

    LaunchedEffect(Unit) {
        delay(3000)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Background Animation
        AnimatedBackground()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = randomVerse,
                style = TextStyle(
                    color = OffWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = Shadow(
                        color = NeuralPurple,
                        blurRadius = 20f
                    )
                ),
                modifier = Modifier.padding(32.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = CyberCyan,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashBG")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BgPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val brush = Brush.linearGradient(
                    colors = listOf(
                        NeuralPurple.copy(alpha = 0.15f),
                        NeonBlue.copy(alpha = 0.1f),
                        VoidBlack
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width * animValue, size.height)
                )
                drawRect(brush)
            }
            .blur(60.dp)
    )
}
