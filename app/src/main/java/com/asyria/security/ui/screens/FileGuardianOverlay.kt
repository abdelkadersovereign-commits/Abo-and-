package com.asyria.security.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asyria.security.ui.theme.*

@Composable
fun FileGuardianOverlay(onClose: () -> Unit) {
    val folders = listOf("ENCRYPTED_LOGS", "NEURAL_SIGNATURES", "CORE_PROTOCOLS", "SENSITIVE_DOCS")
    
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
                .fillMaxHeight(0.7f)
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
                        text = "FILE GUARDIAN",
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

                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(folders.size) { index ->
                        FolderItem(folders[index])
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { /* Placeholder for encryption logic */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = VoidBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ENCRYPT NEW VOLUME", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FolderItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, color = CyberCyan, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = name, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = "Vaulted • 24.5 KB", color = TextGray, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, color = TextGray, modifier = Modifier.size(16.dp))
    }
}
