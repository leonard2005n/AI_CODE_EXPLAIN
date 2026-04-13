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

    fun explainCodeStream(prompt: String, onChunkReceived: (String) -> Unit): ExplanationResult {
        val fullText = StringBuilder()
        var pTokens = 0
        var cTokens = 0
        var tTokens = 0

        try {
            val apiKey = getApiKey()
            if (apiKey.isNullOrBlank()) {
                val errorMsg = "API key is missing. Please set your API key in the settings."
                onChunkReceived(errorMsg)
                return ExplanationResult(text = errorMsg)
            }

            val client = Client.builder().apiKey(apiKey).build()

            // 1. Call the streaming API
            val responseStream = client.models.generateContentStream("gemma-4-31b-it", prompt, null)

            // 2. Iterate over the chunks as they arrive
            for (chunk in responseStream) {
                val textChunk = chunk.text() ?: ""
                if (textChunk.isNotEmpty()) {
                    fullText.append(textChunk)
                    onChunkReceived(textChunk) // Send the partial text back to the action
                }

                // Token usage is typically provided in the final chunk
                chunk.usageMetadata().ifPresent { usage ->
                    pTokens = usage.promptTokenCount().orElse(0)
                    cTokens = usage.candidatesTokenCount().orElse(0)
                    tTokens = usage.totalTokenCount().orElse(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = "\n\n**Error during API call:** ${e.message}"
            fullText.append(errorMsg)
            onChunkReceived(errorMsg)
        }

        // 3. Return the fully aggregated result for history keeping
        return ExplanationResult(
            text = fullText.toString(),
            promptTokens = pTokens,
            candidateTokens = cTokens,
            totalTokens = tTokens
        )
    }

    fun explainCode(prompt: String): ExplanationResult {
        return try {
            val apiKey = getApiKey()

            if (apiKey.isNullOrBlank()) {
                return ExplanationResult(text = "API key is missing. Please set your API key in the settings.")
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