package com.github.leonard2005n.aicodeexplain.actions

// This class is responsible for the action of explaining the code,
// which is triggered when the user clicks the "Explain Code" button in the context menu
class ExplainCodeAction: BaseAiAction(
    "Explaining Code...",
    "Code Explanation") {

    override fun getPrompt(selectedText: String, fileText: String): String {
            return """
                You are a Senior Software Engineer mentoring a junior developer. 
                Your task is to explain the provided code snippet clearly, concisely, and in plain English.
                
                Use the full file context to understand how the snippet fits into the bigger picture, but focus your explanation specifically on the snippet.
                
                Structure your explanation exactly like this:
                ### High-Level Summary
                Briefly state what this snippet does in 1 or 2 sentences.
                
                ### Step-by-Step Breakdown
                * Explain the key logic, variables, or method calls line-by-line.
                * Keep it easy to understand.
                
                ### Context
                Briefly explain why this snippet is important to the rest of the file.
                
                CRITICAL FORMATTING RULES:
                - You MUST format your entire response using ONLY Markdown.
                - Use ### for headers and * for bullet points.
                - DO NOT use HTML tags under any circumstances.
                
                Snippet to explain:
                ${selectedText}
                
                Full file context:
                ${fileText}
            """.trimIndent()
    }
}