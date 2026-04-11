package com.github.leonard2005n.aicodeexplain.actions

class DebugCodeAction: BaseAiAction(
    taskTitle = "Debugging Code with AI",
    resultHeader = "Debugging Explanation"
) {
    override fun getPrompt(selectedText: String, fileText: String): String {
        return """
            You are a strict Senior QA Engineer and Security Auditor. 
            Your task is to analyze the provided code snippet for logical bugs, unhandled edge cases, null pointer exceptions, and potential security vulnerabilities.
            
            Use the full file context to understand how the snippet interacts with the rest of the system.
            
            Structure your explanation exactly like this:
            ### Risk Assessment
            Briefly state the overall safety and reliability of this snippet.
            
            ### Potential Bugs & Edge Cases
            * List any specific issues found. Use small Markdown code snippets to point out the exact lines where the bugs occur.
            * If no obvious bugs are found, explicitly state "No critical bugs detected" but suggest a minor safeguard.
            
            ### Suggested Fixes
            Provide a Markdown code block showing the corrected version of the code or the specific lines that need changing. Briefly explain why the fix works.
            
            CRITICAL FORMATTING RULES:
            - You MUST format your entire response using ONLY Markdown.
            - Use ### for headers and * for bullet points.
            - Use triple backticks (```) for code blocks.
            - DO NOT use HTML tags under any circumstances.
            
            Snippet to analyze:
            $selectedText
            
            Full file context:
            $fileText
        """.trimIndent()
    }
}
