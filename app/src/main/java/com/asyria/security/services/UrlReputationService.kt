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

    // ✅ إصلاح: زيادة timeout إلى 10 ثوانٍ
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT = 10_000

    // ✅ إصلاح: إضافة retry تلقائي عند فشل الاتصال
    private const val MAX_RETRIES = 3

    suspend fun analyzeUrl(urlStr: String): UrlScanResult = withContext(Dispatchers.IO) {
        var lastResult: UrlScanResult? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                lastResult = performAnalysis(urlStr)
                // إذا نجح التحليل نخرج من الـ loop
                return@withContext lastResult!!
            } catch (e: Exception) {
                if (attempt == MAX_RETRIES - 1) {
                    // آخر محاولة فاشلة
                    lastResult = UrlScanResult(
                        isSafe = false,
                        details = listOf(
                            "[INFO] INITIATING LINK AUDIT",
                            "[ERROR] NETWORK UNAVAILABLE AFTER $MAX_RETRIES ATTEMPTS",
                            "[INFO] TRY AGAIN WHEN CONNECTION IS STABLE"
                        ),
                        score = 0
                    )
                }
            }
        }

        lastResult!!
    }

    private fun performAnalysis(urlStr: String): UrlScanResult {
        val details = mutableListOf<String>()
        var score = 100

        details.add("[INFO] INITIATING LINK AUDIT")

        val url = URL(urlStr)
        details.add("[INFO] TARGET: ${url.host}")

        // فحص البروتوكول
        if (url.protocol != "https") {
            details.add("[WARN] INSECURE PROTOCOL (HTTP)")
            score -= 40
        } else {
            details.add("[SECURE] ENCRYPTED PROTOCOL (HTTPS)")
        }

        // فحص الكلمات المشبوهة
        val suspiciousKeywords = listOf(
            "free", "login", "secure", "update",
            "verify", "account", "banking"
        )
        if (suspiciousKeywords.any { url.host.contains(it, ignoreCase = true) }) {
            details.add("[WARN] SUSPICIOUS PHISHING KEYWORD IN DOMAIN")
            score -= 30
        }

        // فحص عناوين IP المباشرة
        if (url.host.matches(Regex("^[0-9]+(\\.[0-9]+){3}\$"))) {
            details.add("[WARN] OBFUSCATED IP ADDRESS ROUTING DETECTED")
            score -= 50
        }

        // ✅ إصلاح: فحص SSL مع timeout أطول
        if (url.protocol == "https") {
            try {
                val connection = url.openConnection() as HttpsURLConnection
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
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

        details.add("[INFO] REPUTATION ANALYSIS COMPLETE")

        return UrlScanResult(
            isSafe = score >= 70,
            details = details,
            score = score.coerceIn(0, 100)
        )
    }
}
