package com.github.leonard2005n.aicodeexplain.actions

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
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

abstract class BaseAiAction(
    private val taskTitle: String,
    private val resultHeader: String
): AnAction() {

    abstract fun getPrompt(selectedText: String, fileText: String): String

    override fun actionPerformed(e: AnActionEvent) {

        // Get the Project
        val project = e.project ?: return

        // Get the Editor and the highlighted text
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        // Capture only the necessary data from the EDT
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: return
        
        if (selectedText.isBlank()) {
            return
        }

        // Automatically open the Tool Window so the user sees something is happening
        ToolWindowManager.getInstance(project).getToolWindow("AI Explainer")?.show()

        // We capture the document and offsets instead of the whole text on the EDT
        val document = editor.document
        val projectService = project.service<MyProjectService>()

        projectService.setLoading(true)

        // Run the explanation in a background task to avoid freezing the UI
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, taskTitle) {
            override fun run(indicator: ProgressIndicator) {
                // 1. Get the full file text in the background thread
                // Note: document.text is safe to call in background if we don't need a specific read action (usually it's fine for just reading)
                // However, for maximum safety in IntelliJ, we should use a read action if needed, but simple .text is often okay.
                val fileText = document.text

                // 2. Prepare the highly optimized prompt (now in background!)
                var prompt = getPrompt(selectedText, fileText)

                val titleInstruction = """
                    CRITICAL INSTRUCTION: You MUST start your response with a 3 to 6 word title summarizing this specific request.
                    Wrap the title exactly in these tags: [TITLE] Your Title Here [/TITLE]. 
                    Do not put any other text before the [TITLE] tag.
                    After the [/TITLE] tag, leave a blank line and provide your actual Markdown response.
                    
                """.trimIndent()

                prompt = titleInstruction + prompt

                // 3. Call the GeminiService to get the explanation
                val geminiService = ApplicationManager.getApplication().service<GeminiService>()
                val explanation = geminiService.explainCode(prompt)

                // 4. Process the result in the background
                var rawMarkdown = explanation.text
                var aiTitle = "$resultHeader snippet"

                //
                val titleStart = rawMarkdown.indexOf("[TITLE]")
                val titleEnd = rawMarkdown.indexOf("[/TITLE]")

                if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) {
                    // Pull out the text between the tags
                    aiTitle = rawMarkdown.substring(titleStart + 7, titleEnd).trim()
                    // Remove the tags and the title from the markdown we render
                    rawMarkdown = rawMarkdown.substring(titleEnd + 8).trim()
                }

                // 4. Parse the Markdown safely into clean HTML
                val parser = Parser.builder().build()
                val document = parser.parse(rawMarkdown)
                val renderer = HtmlRenderer.builder().build()
                val safeAiHtml = renderer.render(document)

                // 5. Wrap it in your final layout
                val finalHtml = """
                                    <html>
                                    <body style="font-family: sans-serif; padding: 10px;">
                                        <h3 style="color: #888888;">&#128269; $resultHeader :</h3>
                                        <hr>
                                        <pre style="background-color: #2b2b2b; color: #a9b7c6; padding: 10px; white-space: pre-wrap; word-wrap: break-word;"><code>${selectedText.replace("<", "&lt;").replace(">", "&gt;")}</code></pre>
                                        <hr><br>
                                        <div style="line-height: 1.4;">$safeAiHtml</div>
                                        
                                        <div style="margin-top: 30px; padding-top: 10px; border-top: 1px dashed #555555; color: #999999; font-size: 10px; font-family: monospace;">
                                            <b style="color: #888888;">Token usage</b><br>
                                            Input: ${explanation.promptTokens} | Output: ${explanation.candidateTokens} | Total: ${explanation.totalTokens}
                                        </div>
                                    </body>
                                    </html>
                                """.trimIndent()

                // 6. Update the UI on the EDT
                ApplicationManager.getApplication().invokeLater {
                    val projectService = project.service<MyProjectService>()
                    projectService.addToHistory(finalHtml, aiTitle)
                }
            }
            override fun onFinished() {
                projectService.setLoading(false)
            }
        })
    }

    // This ensures the button is ONLY clickable if text is highlighted
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor?.selectionModel?.hasSelection() == true
    }
}