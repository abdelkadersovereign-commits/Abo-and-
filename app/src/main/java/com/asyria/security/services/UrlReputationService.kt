package com.asyria.security.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object UrlReputationService {

    data class UrlScanResult(
        val isSafe: Boolean,
        val details: List<String>,
        val score: Int
    )

    suspend fun analyzeUrl(urlStr: String): UrlScanResult = withContext(Dispatchers.IO) {
        val details = mutableListOf<String>()
        var score = 100

        details.add("[INFO] INITIATING LINK AUDIT")

        try {
            val url = URL(urlStr)
            details.add("[INFO] TARGET: ${url.host}")

            if (url.protocol != "https") {
                details.add("[WARN] INSECURE PROTOCOL (HTTP)")
                score -= 40
            } else {
                details.add("[SECURE] ENCRYPTED PROTOCOL (HTTPS)")
            }

            // Simple homoglyph/suspicious check
            val suspiciousKeywords = listOf("free", "login", "secure", "update", "verify", "account", "banking")
            if (suspiciousKeywords.any { url.host.contains(it, ignoreCase = true) }) {
                details.add("[WARN] SUSPICIOUS PHISHING KEYWORD IN DOMAIN")
                score -= 30
            }

            if (url.host.matches(Regex("^[0-9]+(\\.[0-9]+){3}\$"))) {
                details.add("[WARN] OBFUSCATED IP ADDRESS ROUTING DETECTED")
                score -= 50
            }

            // Real HTTPS Check
            if (url.protocol == "https") {
                try {
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.connect()
                    
                    val certs = connection.serverCertificates
                    if (certs.isNotEmpty()) {
                        details.add("[SECURE] SSL CERTIFICATE VERIFIED")
                    } else {
                        details.add("[WARN] MISSING CERTIFICATE CHAIN")
                        score -= 50
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    details.add("[WARN] SSL NEGOTIATION FAILED OR TIMEOUT")
                    score -= 40
                }
            }

        } catch (e: Exception) {
            details.add("[ERROR] MALFORMED URL OR UNREACHABLE")
            score = 0
        }

        details.add("[INFO] REPUTATION ANALYSIS COMPLETE")

        UrlScanResult(
            isSafe = score >= 70,
            details = details,
            score = score.coerceIn(0, 100)
        )
    }
}
