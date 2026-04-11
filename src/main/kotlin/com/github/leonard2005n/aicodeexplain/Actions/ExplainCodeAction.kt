package com.github.leonard2005n.aicodeexplain.Actions

import com.github.leonard2005n.aicodeexplain.services.GeminiService
import com.github.leonard2005n.aicodeexplain.services.MyProjectService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.wm.ToolWindowManager

// This class is responsible for the action of explaining the code,
// which is triggered when the user clicks the "Explain Code" button in the context menu
class ExplainCodeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        // Get the Project
        val project = e.project ?: return

        // Get the Editor and the highlighted text
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        // Getting the code for the service
        val selectedText = editor.selectionModel.selectedText ?: return
        val fileText = editor.document.text

        if (selectedText.isBlank()) {
            return
        }

        // 2. Prepare the highly optimized prompt
        val prompt = """
                You are a Senior Software Engineer mentoring a junior developer. 
                Your task is to explain the provided code snippet clearly, concisely, and in plain English.
                
                Use the full file context to understand how the snippet fits into the bigger picture, but focus your explanation specifically on the snippet.
                
                Structure your explanation exactly like this:
                <p><b>High-Level Summary:</b> Briefly state what this snippet does in 1 or 2 sentences.</p>
                <p><b>Step-by-Step Breakdown:</b></p>
                <ul>
                    <li>Explain the key logic, variables, or method calls line-by-line.</li>
                    <li>Keep it easy to understand.</li>
                </ul>
                <p><b>Context:</b> Briefly explain why this snippet is important to the rest of the file.</p>
                
                CRITICAL FORMATTING RULES:
                - You MUST format your entire response using ONLY basic HTML tags.
                - Allowed tags: <b>, <i>, <br>, <p>, <ul>, <li>, <code>.
                - DO NOT use Markdown under any circumstances (NO asterisks **, NO backticks `, NO hashes #).
                
                Snippet to explain:
                $selectedText
                
                Full file context:
                $fileText
            """.trimIndent()

        // Run the explanation in a background task to avoid freezing the UI
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Explaining Code...") {
            override fun run(indicator: ProgressIndicator) {
                // 2. Call the GeminiService to get the explanation
                // Note: Ensure GeminiService is registered as an application-level service in plugin.xml
                val geminiService = ApplicationManager.getApplication().service<GeminiService>()
                val explanation = geminiService.explainCode(prompt)

                // 3. Show the explanation in the Tool Window
                ApplicationManager.getApplication().invokeLater {
                    val formattedText = """
                       <html>
                       <body style="font-family: sans-serif; padding: 10px;">
                            <h3 style="color: #888888;">&#128269; ANALYZED SNIPPET:</h3>
                        <hr>
                        <pre style="background-color: #2b2b2b; padding: 10px;"><code>${selectedText.replace("<", "&lt;").replace(">", "&gt;")}</code></pre>
                        <hr><br>
            
                        <h3 style="color: #888888;">&#128161; AI EXPLANATION:</h3>
                        <div style="line-height: 1.4;">${explanation.text}</div>
            
                        <hr>
                            <p style="color: #888888; font-size: 10px;">
                                Tokens used: <b>${explanation.totalTokens}</b> 
                                (Input: ${explanation.promptTokens}, Output: ${explanation.candidateTokens})
                            </p>
                        </body>
                        </html>
                    """.trimIndent()

                    val projectService = project.service<MyProjectService>()
                    projectService.updateExplanation(formattedText)

                    // IMPORTANT: Ensure this string matches the 'id' you set in plugin.xml
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("AI Explainer")
                    toolWindow?.show()
                }
            }
        })
    }

    // This ensures the button is ONLY clickable if text is highlighted
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor?.selectionModel?.hasSelection() == true
    }
}