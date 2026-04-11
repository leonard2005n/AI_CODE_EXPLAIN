package com.github.leonard2005n.aicodeexplain.services

import com.google.genai.Client
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service

data class ExplanationResult(
    val text: String,
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val totalTokens: Int = 0
)

// Class for the gemini api, which is used to get the explanation of the code
@Service(Service.Level.APP)
class GeminiService {

    private val API_KEY_STORAGE_ID = "com.github.leonard2005n.aicodeexplain.GEMINI_API_KEY"

    fun setApiKey(key: String) {
        // Use PropertiesComponent to save the key permanently across IDE restarts
        PropertiesComponent.getInstance().setValue(API_KEY_STORAGE_ID, key)
    }

    fun getApiKey(): String? {
        // Retrieve the permanently saved key
        return PropertiesComponent.getInstance().getValue(API_KEY_STORAGE_ID)
    }

    fun explainCode(prompt: String): ExplanationResult {
        return try {
            val apiKey = getApiKey()

            if (apiKey.isNullOrBlank()) {
                ExplanationResult(text = "API key is missing. Please set your API key in the settings.")
            }

            // 1. Build the simple client using the actual retrieved key
            val client = Client.builder().apiKey(apiKey).build()

            // 2. Prepare the prompt
            val response = client.models.generateContent("gemma-4-31b-it", prompt, null)

            // Extract token usage details from the response
            val usage = response.usageMetadata().orElse(null)

            // 3. Return the explanation text along with token usage details
            ExplanationResult(
                text = response.text() ?: "No explanation available.",
                promptTokens = usage?.promptTokenCount()?.orElse(0) ?: 0,
                candidateTokens = usage?.candidatesTokenCount()?.orElse(0) ?: 0,
                totalTokens = usage?.totalTokenCount()?.orElse(0) ?: 0
            )


        } catch (e: Exception) {
            e.printStackTrace()
            ExplanationResult(text = "Error during API call: ${e.message}")
        }
    }
}