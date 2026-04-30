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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.asyria.security.data.prayer.SupplicationEntity

import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SpiritualHub(
    onClose: () -> Unit,
    viewModel: PrayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
                Crossfade(targetState = activeTab, label = "TabContent") { tab ->
                    when(tab) {
                        0 -> DetailedPrayerTimes(uiState)
                        1 -> EnhancedAzkarModule(uiState.supplications)
                    }
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
fun DetailedPrayerTimes(uiState: PrayerUiState) {
    val timings = uiState.timings
    val prayers = remember(timings) {
        if (timings == null) emptyList()
        else listOf(
            "Fajr" to timings.Fajr,
            "Dhuhr" to timings.Dhuhr,
            "Asr" to timings.Asr,
            "Maghrib" to timings.Maghrib,
            "Isha" to timings.Isha
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Next Prayer Hero Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(32.dp),
            color = AmberZen.copy(alpha = 0.05f),
            border = BorderStroke(2.dp, Brush.linearGradient(listOf(AmberZen, Color.Transparent, AmberZen)))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NEXT: ${uiState.nextPrayerName.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberZen,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.countdown,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Light,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "DAILY INTERVALS",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(prayers.size) { index ->
                val (name, time) = prayers[index]
                val isNext = name == uiState.nextPrayerName
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isNext) AmberZen.copy(alpha = 0.15f) else GlassWhite,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isNext) AmberZen else GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            color = if (isNext) AmberZen else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = time,
                            color = if (isNext) AmberZen else TextGray,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedAzkarModule(supplications: List<SupplicationEntity>) {
    var selectedCategory by remember { mutableStateOf("Soul Calming") }
    val categories = listOf("Morning", "Evening", "Soul Calming", "Misc")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Category Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    modifier = Modifier.clickable { selectedCategory = cat },
                    color = if (isSelected) AmberZen else GlassWhite,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) AmberZen else GlassBorder)
                ) {
                    Text(
                        text = cat,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isSelected) VoidBlack else Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filtered = supplications.filter { it.category == selectedCategory }
            items(filtered.size) { index ->
                SupplicationCard(filtered[index])
            }
        }
    }
}

@Composable
fun SupplicationCard(item: com.asyria.security.data.prayer.SupplicationEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassWhite,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = item.resonance.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AmberZen,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.content,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.translation,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                lineHeight = 16.sp
            )
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
