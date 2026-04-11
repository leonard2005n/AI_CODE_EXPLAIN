package com.github.leonard2005n.aicodeexplain.services

import com.google.genai.Client
import com.intellij.openapi.components.Service
import com.jetbrains.rd.framework.SocketWire

// Class for the gemeni api, which is used to get the explanation of the code
// The gemeni api is a wrapper around the gemeni api, which is used to get the explanation of the code
@Service(Service.Level.APP)
class GeminiService {

    // The api key of the gemini api, which is used to get the explanation of the code
    private val apiKey = "Your API key here"

    fun explainCode(snippet: String, code : String): String {
        return try {
            // 1. Build the simple client
            val client = Client.builder().apiKey(apiKey).build()

            // 2. Prepare the prompt
            val prompt = "You are an expert developer. Explain the following code " +
                    "concisely and clearly in plain English. I will give you" +
                    "a snippet of a code and you will explain it to me. Here is the code: \n\n$snippet" +
                    "and the rest of the file to give you more context: \n\n$code"

            val response = client.models.generateContent("gemma-4-31b-it", prompt, null)

            // 3. Return the explanation
            response.text() ?: "No explanation available."

        } catch (e: Exception) {
            e.printStackTrace()
            return "An error occurred while trying to explain the code: ${e.message}"
        }
    }
}