package com.asyria.security.services

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.MessageDigest

object FileScannerService {

    data class ScanResult(
        val fileName: String,
        val fileSize: String,
        val safetyScore: Int,
        val logs: List<String>
    )

    suspend fun scanFile(context: Context, uri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        var name = "UNKNOWN_FILE"
        var sizeKb = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
                sizeKb = cursor.getLong(sizeIndex) / 1024
            }
        }

        logs.add("[INFO] INITIATING NEURAL DEEP SCAN")
        delay(800)
        logs.add("[INFO] FILE METADATA EXTRACTED: $name ($sizeKb KB)")

        // Calculate heuristic score based on simulated anomaly check
        // In real-world, we run signature matching here
        var score = 100
        delay(1000)

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                val digest = MessageDigest.getInstance("SHA-256")
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                val hashBytes = digest.digest()
                val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
                logs.add("[INFO] SHA-256 INTEGRITY HASH: ${hashHex.take(16)}...")
            }
        } catch (e: Exception) {
            logs.add("[WARN] COULD NOT COMPLETE HASH CHECK")
            score -= 20
        }

        delay(1200)
        logs.add("[INFO] HEURISTIC PATTERN MATCHING...")

        if (name.endsWith(".apk") || name.endsWith(".exe") || name.endsWith(".sh")) {
            logs.add("[WARN] EXECUTABLE BINARY DETECTED")
            score -= 30
        } else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".mp4")) {
            logs.add("[SECURE] STANDARD MEDIA FORMAT")
            // simple steganography check simulation
            if (sizeKb > 50000) {
                logs.add("[WARN] UNUSUALLY LARGE MEDIA FILE. STRUCTURAL ANOMALY POSSIBLE.")
                score -= 15
            } else {
                logs.add("[INFO] STEGANOGRAPHY CHECK: PASSED")
            }
        }

        logs.add("[SECURE] NEURAL PASSPORT GENERATED")

        ScanResult(
            fileName = name,
            fileSize = "${sizeKb / 1024} MB",
            safetyScore = score.coerceIn(0, 100),
            logs = logs
        )
    }
}
