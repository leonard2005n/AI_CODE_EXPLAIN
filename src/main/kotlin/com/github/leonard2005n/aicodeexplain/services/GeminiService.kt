package com.github.leonard2005n.aicodeexplain.services

import com.google.genai.Client
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service

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

    fun explainCode(prompt: String): String {
        return try {
            val apiKey = getApiKey()

            if (apiKey.isNullOrBlank()) {
                return "API key not set. Please set the API key in the AI Explainer tab."
            }

            // 1. Build the simple client using the actual retrieved key
            val client = Client.builder().apiKey(apiKey).build()

            // 2. Prepare the prompt
            val response = client.models.generateContent("gemma-4-31b-it", prompt, null)

            // 3. Return the explanation
            response.text() ?: "No explanation available."

        } catch (e: Exception) {
            e.printStackTrace()
            return "An error occurred while trying to explain the code: ${e.message}"
        }
    }
}