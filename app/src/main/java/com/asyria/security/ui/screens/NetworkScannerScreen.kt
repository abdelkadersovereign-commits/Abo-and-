package com.asyria.security.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat

@Composable
fun NetworkScannerScreen(
    onClose: () -> Unit,
    viewModel: ThreatScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        val ip = getLocalIpAddress() ?: "127.0.0.1"
        val type = getNetworkType(context)
        viewModel.setNetworkInfo(ip, type)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VoidBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "THREAT ANALYSIS",
                        style = MaterialTheme.typography.titleLarge,
                        color = CyberCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "NEURAL NETWORK AUDIT",
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
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }

            // Persistence Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .background(GlassWhite, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("LIVE MONITOR", "THREAT LEDGER").forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (uiState.activeTab == index) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setTab(index) 
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (uiState.activeTab == index) CyberCyan else TextGray,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Crossfade(targetState = uiState.activeTab, label = "TabSwitch") { tab ->
                when (tab) {
                    0 -> LiveMonitorView(uiState, viewModel.logs)
                    1 -> ThreatLedgerView(uiState.threatLogs, onPurge = { viewModel.purgeLogs() })
                }
            }
        }
    }
}

@Composable
fun LiveMonitorView(uiState: ThreatScannerUiState, logs: List<String>) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Radar Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarSweep()
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LOCAL_NODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = uiState.localIp,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Live Traffic Graph
        Text(
            text = "LIVE FLOW DENSITY",
            style = MaterialTheme.typography.labelMedium,
            color = TextGray,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        LiveTrafficGraph(uiState.trafficData)

        Spacer(modifier = Modifier.height(16.dp))

        // Tech Specs Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoTile(Modifier.weight(1f), "UPLINK", uiState.networkType, Icons.Default.Wifi)
            InfoTile(Modifier.weight(1f), "STATUS", "STABLE", Icons.Default.Security)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // The Matrix Log View
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
            color = GlassWhite
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    val color = when {
                        log.contains("[DANGER]") -> RiskRed
                        log.contains("[SECURE]") -> CyberCyan.copy(alpha = 0.7f)
                        else -> TextGray
                    }
                    Text(
                        text = log,
                        color = color,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatLedgerView(threats: List<ThreatEntry>, onPurge: () -> Unit) {
    var isPurging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PERSISTENT AUDIT LOG",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )
            
            TextButton(
                onClick = { 
                    scope.launch {
                        isPurging = true
                        delay(800)
                        onPurge()
                        isPurging = false
                    }
                },
                enabled = threats.isNotEmpty() && !isPurging
            ) {
                Text(
                    text = if (isPurging) "PURGING..." else "PURGE LEDGER",
                    color = if (threats.isNotEmpty()) RiskRed else TextGray,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (threats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("NO ANOMALIES DETECTED", color = TextGray.copy(alpha = 0.5f))
            }
        } else {
            AnimatedVisibility(
                visible = !isPurging,
                enter = fadeIn(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(threats, key = { it.id }) { threat ->
                        ThreatEntryCard(threat)
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatEntryCard(threat: ThreatEntry) {
    val borderColor = when (threat.severity) {
        ThreatSeverity.CRITICAL -> RiskRed
        ThreatSeverity.MEDIUM -> AmberZen
        ThreatSeverity.LOW -> CyberCyan
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlassWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = threat.type,
                    color = borderColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = threat.severity.name,
                    color = borderColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = TextGray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(threat.timestamp, color = TextGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Language, null, tint = TextGray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(threat.sourceIp, color = TextGray, fontSize = 11.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = threat.description,
                color = OffWhite.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun LiveTrafficGraph(data: List<Float>) {
    val infiniteTransition = rememberInfiniteTransition(label = "GraphGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite.copy(alpha = 0.05f))
    ) {
        if (data.size < 2) return@Canvas
        
        val width = size.width
        val height = size.height
        val maxVal = 150f
        val stepX = width / (data.size - 1)

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height - (data[0] / maxVal * height))
            data.forEachIndexed { index, value ->
                if (index > 0) {
                    lineTo(index * stepX, height - (value / maxVal * height))
                }
            }
        }

        drawPath(
            path = path,
            color = CyberCyan.copy(alpha = glowAlpha),
            style = Stroke(width = 4f, pathEffect = PathEffect.cornerPathEffect(20f))
        )
        
        // Shadow fill
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(CyberCyan.copy(alpha = 0.2f), Color.Transparent)
            )
        )
    }
}


@Composable
fun RadarSweep() {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing))
    )

    Canvas(modifier = Modifier.size(240.dp)) {
        // Grid Lines
        val stroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        
        drawCircle(
            brush = Brush.radialGradient(listOf(CyberCyan.copy(alpha = 0.1f), Color.Transparent)),
            radius = size.width / 2
        )
        
        drawCircle(color = CyberCyan.copy(alpha = 0.1f), style = stroke, radius = size.width / 2)
        drawCircle(color = CyberCyan.copy(alpha = 0.1f), style = stroke, radius = size.width / 3)
        drawCircle(color = CyberCyan.copy(alpha = 0.1f), style = stroke, radius = size.width / 6)
        
        drawLine(
            color = CyberCyan.copy(alpha = 0.1f),
            start = Offset(0f, size.height/2),
            end = Offset(size.width, size.height/2),
            style = stroke
        )
        drawLine(
            color = CyberCyan.copy(alpha = 0.1f),
            start = Offset(size.width/2, 0f),
            end = Offset(size.width/2, size.height),
            style = stroke
        )

        // Sweep
        drawArc(
            brush = Brush.sweepGradient(
                0f to Color.Transparent,
                1f to CyberCyan.copy(alpha = 0.4f)
            ),
            startAngle = rotation,
            sweepAngle = 90f,
            useCenter = true
        )
    }
}

@Composable
fun InfoTile(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = modifier,
        color = GlassWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextGray)
                Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getLocalIpAddress(): String? {
    try {
        val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf: NetworkInterface = en.nextElement()
            val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress: InetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress.getHostAddress().contains('.')) {
                    return inetAddress.getHostAddress().toString()
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null
}

private fun getNetworkType(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork ?: return "OFFLINE"
    val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "NONE"
    
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI_AX"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELL_5G"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETH_CORE"
        else -> "UNKNOWN"
    }
}
