package com.github.leonard2005n.aicodeexplain.actions

class GenerateTestsAction: BaseAiAction(
    taskTitle = "Generating Unit Tests with AI",
    resultHeader = "Generated Unit Tests for Snippet"
) {
    override fun getPrompt(selectedText: String, fileText: String): String {
        return """
            You are a highly skilled Senior Software Engineer specializing in test-driven development. 
            Your primary task is to write the ACTUAL, ready-to-run unit test code for the provided snippet.
            
            Use the full file context to determine the correct testing framework, imports, and mocks required.
            
            Structure your response EXACTLY like this:
            ### Test Coverage Summary
            Briefly state what scenarios your code tests.
            
            ### Generated Test Code
            ```
            [INSERT THE COMPLETE, EXECUTABLE TEST CLASS/METHODS HERE]
            ```
            
            CRITICAL FORMATTING RULES:
            - Your main output MUST be the actual test code inside the Markdown code block.
            - Do NOT just describe the tests; you must write the actual code.
            - You MUST format your entire response using ONLY Markdown.
            - Use ### for headers.
            - DO NOT use HTML tags under any circumstances.
            
            Snippet to test:
            $selectedText
            
            Full file context:
            $fileText
        """.trimIndent()
    }
}