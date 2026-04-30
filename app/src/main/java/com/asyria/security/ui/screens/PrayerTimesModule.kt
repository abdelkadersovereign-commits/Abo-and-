package com.asyria.security.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.font.FontFamily
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.style.TextOverflow

// Damascus Coordinates approx: Latitude: 33.5138, Longitude: 36.2765
// Here we will do a very simplified hardcoded calculation based on standard Damascus timezone (GMT+3)
// For a production app, proper astronomical calculation would be used.
// We will generate fake logical times around the current day for Damascus.

data class DailyPrayer(val name: String, val timeStr: String, val dateHour: Int, val dateMin: Int)

fun calculateDamascusPrayers(calendar: Calendar): List<DailyPrayer> {
    // Fake logic for Damascus GMT+3:
    // Fajr ~ 4:30
    // Dhuhr ~ 12:35
    // Asr ~ 16:15
    // Maghrib ~ 19:20
    // Isha ~ 20:50
    return listOf(
        DailyPrayer("Fajr", "04:30 AM", 4, 30),
        DailyPrayer("Dhuhr", "12:35 PM", 12, 35),
        DailyPrayer("Asr", "04:15 PM", 16, 15),
        DailyPrayer("Maghrib", "07:20 PM", 19, 20),
        DailyPrayer("Isha", "08:50 PM", 20, 50)
    )
}

@Composable
fun PrayerTimesModule(onClose: () -> Unit) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    var countdownText by remember { mutableStateOf("") }
    var nextPrayerName by remember { mutableStateOf("") }

    val prayers = remember { calculateDamascusPrayers(currentTime) }
    
    val verses = remember {
        listOf(
            "ألا بذكر الله تطمئن القلوب",
            "إن الله مع الصابرين",
            "وقل ربي زدني علماً",
            "ادعوني أستجب لكم"
        )
    }
    
    var verseIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(10000)
            verseIndex = (verseIndex + 1) % verses.size
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance(TimeZone.getTimeZone("GMT+3")) // Damascus Time
            currentTime = now

            // Calculate next prayer countdown
            var nextPr: DailyPrayer? = null
            for (p in prayers) {
                if (now.get(Calendar.HOUR_OF_DAY) < p.dateHour || 
                   (now.get(Calendar.HOUR_OF_DAY) == p.dateHour && now.get(Calendar.MINUTE) < p.dateMin)) {
                    nextPr = p
                    break
                }
            }
            if (nextPr == null) nextPr = prayers[0] // Next day's fajr
            
            nextPrayerName = nextPr.name
            
            var diffHour = nextPr.dateHour - now.get(Calendar.HOUR_OF_DAY)
            var diffMin = nextPr.dateMin - now.get(Calendar.MINUTE)
            var diffSec = 60 - now.get(Calendar.SECOND)
            if (diffSec == 60) { diffSec = 0 } else { diffMin -= 1 }
            if (diffMin < 0) { diffMin += 60; diffHour -= 1 }
            if (diffHour < 0) { diffHour += 24 }

            countdownText = String.format("%02d:%02d:%02d", diffHour, diffMin, diffSec)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.9f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(VoidBlack)
                .border(2.dp, AmberZen.copy(alpha = 0.5f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "A.SYRIA - SPIRITUAL HUB",
                    style = MaterialTheme.typography.titleLarge,
                    color = AmberZen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(GlassWhite, CircleShape).size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AmberZen)
                }
            }

            // Verse Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = verses[verseIndex],
                    transitionSpec = {
                        fadeIn(animationSpec = tween(2000)) togetherWith fadeOut(animationSpec = tween(2000))
                    },
                    label = "VerseAnim"
                ) { verse ->
                    Text(
                        text = verse,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(AmberZen.copy(alpha = 0.3f), Color.Transparent),
                                    center = center,
                                    radius = size.maxDimension / 2
                                ),
                                blendMode = BlendMode.Screen
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Countdown
            Surface(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(24.dp),
                color = AmberZen.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, AmberZen.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NEXT: ${nextPrayerName.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberZen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.displayMedium,
                        fontFamily = FontFamily.Monospace,
                        color = OffWhite,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "DAILY PRAYERS",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                modifier = Modifier.align(Alignment.Start),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(prayers.size) { i ->
                    val p = prayers[i]
                    val isNext = p.name == nextPrayerName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isNext) AmberZen.copy(alpha = 0.2f) else GlassWhite,
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (isNext) AmberZen else GlassBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isNext) AmberZen else OffWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = p.timeStr,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isNext) AmberZen else TextGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
