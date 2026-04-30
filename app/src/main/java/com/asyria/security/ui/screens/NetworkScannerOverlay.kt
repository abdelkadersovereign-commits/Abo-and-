package com.asyria.security.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun NetworkScannerOverlay(onClose: () -> Unit) {
    var isScanning by remember { mutableStateOf(true) }
    val connections = remember {
        listOf(
            "UPLINK_SY_01" to "192.168.1.45",
            "NEURAL_GATE" to "10.0.0.12",
            "GHOST_NODE" to "172.16.254.1",
            "SECURE_BUFFER" to "192.168.1.1"
        )
    }

    LaunchedEffect(Unit) {
        delay(3000)
        isScanning = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.9f))
            .clickable(onClick = onClose)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                .clickable(enabled = false) {},
            color = VoidBlack,
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NETWORK TOPOLOGY",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isScanning) {
                    ScanningAnimation()
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(connections.size) { index ->
                            ConnectionItem(connections[index].first, connections[index].second)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningAnimation() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = CyberCyan, strokeWidth = 2.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "MAPING NODE VECTORS...",
            color = CyberCyan,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun ConnectionItem(name: String, ip: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (name == "GHOST_NODE") RiskRed else SuccessGreen)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = name, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = ip, color = TextGray, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (name == "GHOST_NODE") "MALICIOUS?" else "ENCRYPTED",
            color = if (name == "GHOST_NODE") RiskRed else CyberCyan,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
