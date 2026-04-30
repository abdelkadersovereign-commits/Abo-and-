package com.asyria.security.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun SpiritualHub(onClose: () -> Unit) {
    var activeTab by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VoidBlack.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Animated Header
            HeaderBlock(onClose)

            // Dynamic Content Area
            Box(modifier = Modifier.weight(1f)) {
                when(activeTab) {
                    0 -> PrayerTimesModule(onClose = {}) // Already implemented in its own file
                    1 -> AzkarModule()
                }
            }

            // High-Tech Bottom Nav
            SpiritualBottomNav(
                currentTab = activeTab,
                onTabChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    activeTab = it 
                }
            )
        }
    }
}

@Composable
fun HeaderBlock(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "NEURAL SPIRITUAL HUB",
                style = MaterialTheme.typography.titleSmall,
                color = AmberZen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Syncing Soul & System",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .clip(CircleShape)
                .background(GlassWhite)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = AmberZen)
        }
    }
}

@Composable
fun AzkarModule() {
    val azkar = remember {
        listOf(
            "أستغفر الله العظيم وأتوب إليه" to "Quest for Absolution",
            "سبحان الله وبحمده" to "Infinite Resonancy",
            "لا إله إلا الله" to "Universal Oneness",
            "اللهم صل على محمد" to "Divine Connection"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DAILY RECURRENCES",
            style = MaterialTheme.typography.labelMedium,
            color = TextGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(azkar.size) { index ->
                AzkarItem(azkar[index].first, azkar[index].second)
            }
        }
    }
}

@Composable
fun AzkarItem(arabic: String, title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassWhite,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberZen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, color = AmberZen, contentDescription = null)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberZen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = arabic,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpiritualBottomNav(currentTab: Int, onTabChange: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        color = GlassWhite,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SpiritualNavItem(
                icon = Icons.Default.Schedule,
                label = "Times",
                isSelected = currentTab == 0,
                onClick = { onTabChange(0) }
            )
            SpiritualNavItem(
                icon = Icons.Default.AutoAwesome,
                label = "Daily",
                isSelected = currentTab == 1,
                onClick = { onTabChange(1) }
            )
        }
    }
}

@Composable
fun SpiritualNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.4f, label = "Alpha")
    val color = if (isSelected) AmberZen else TextGray

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color.copy(alpha = alpha))
        Text(
            text = label,
            color = color.copy(alpha = alpha),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
