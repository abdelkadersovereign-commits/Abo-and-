package com.asyria.security.services

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SentinelAiService(apiKey: String) {
    private val model = GenerativeModel(
        modelName = "gemini-3.0-flash-preview",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 1000
        },
        systemInstruction = content {
            text("You are SENTINEL AI, the core intelligence of the A.SYRIA Security App. " +
                 "Your goal is to assist the operator with security analysis, explain app features, " +
                 "and maintain a professional, high-tech, slightly futuristic persona. " +
                 "You specialize in identifying protocol threats and system optimizations. " +
                 "Keep your responses concise and technically accurate.")
        }
    )

    private val chat = model.startChat()

    suspend fun generateResponse(prompt: String): String {
        return try {
            val response = chat.sendMessage(prompt)
            response.text ?: "Neural link failure: Empty response generated."
        } catch (e: Exception) {
            "Neural link failure: ${e.message}"
        }
    }
}
