package com.github.leonard2005n.aicodeexplain.actions

import com.github.leonard2005n.aicodeexplain.services.GeminiService
import com.github.leonard2005n.aicodeexplain.services.ExplanationResult
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
import java.util.concurrent.ConcurrentLinkedQueue

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
        val editorDocument = editor.document
        val projectService = project.service<MyProjectService>()

        projectService.setLoading(true)

        // Run the explanation in a background task to avoid freezing the UI
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, taskTitle) {
            override fun run(indicator: ProgressIndicator) {
                // 1. Get the full file text in the background thread
                val fileText = editorDocument.text

                // 2. Prepare the highly optimized prompt (now in background!)
                var prompt = getPrompt(selectedText, fileText)

                val titleInstruction = """
                    CRITICAL INSTRUCTION: You MUST start your response with a 3 to 6 word title summarizing this specific request.
                    Wrap the title exactly in these tags: [TITLE] Your Title Here [/TITLE]. 
                    Do not put any other text before the [TITLE] tag.
                    After the [/TITLE] tag, leave a blank line and provide your actual Markdown response.
                    
                """.trimIndent()

                prompt = titleInstruction + prompt

                // 3. Setup Gemini Service and Markdown parsers
                val geminiService = ApplicationManager.getApplication().service<GeminiService>()
                val parser = Parser.builder().build()
                val renderer = HtmlRenderer.builder().build()

                var charQueue = ConcurrentLinkedQueue<Char>()
                var isStreamFinished = false
                val displayedMarkdown = StringBuilder()

                // Helper function to process the chunk, render HTML, and dispatch to UI thread
                fun updateUI(currentMarkdown: String, isFinal: Boolean, usage: ExplanationResult? = null) {
                    var rawMarkdown = currentMarkdown
                    var aiTitle = "$resultHeader snippet"

                    // Handle the TITLE tags dynamically during the stream
                    val titleStart = rawMarkdown.indexOf("[TITLE]")
                    val titleEnd = rawMarkdown.indexOf("[/TITLE]")

                    if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) {
                        // Both tags are present: Pull out the title and remove tags from markdown
                        aiTitle = rawMarkdown.substring(titleStart + 7, titleEnd).trim()
                        rawMarkdown = rawMarkdown.substring(titleEnd + 8).trim()
                    } else if (titleStart != -1 && !isFinal) {
                        // Edge case during stream: The start tag arrived, but not the end tag yet.
                        // We hide the partial "[TITLE] Gen..." text until the closing tag arrives.
                        rawMarkdown = "Generating response..."
                    }

                    if (!isFinal) {
                        rawMarkdown += "CURSOR_PLACEHOLDER"
                    }

                    // Parse the accumulated Markdown safely into clean HTML
                    val mdDocument = parser.parse(rawMarkdown)
                    var safeAiHtml = renderer.render(mdDocument)

                    // 2. Replace the placeholder with the styled HTML cursor inside the rendered HTML
                    if (!isFinal) {
                        safeAiHtml = safeAiHtml.replace("CURSOR_PLACEHOLDER", "<span style='color: #888888;'>&#9608;</span>")
                    }

                    // Only show tokens when finished
                    val tokenHtml = if (isFinal && usage != null) {
                        """<div style="margin-top: 30px; padding-top: 10px; border-top: 1px dashed #555555; color: #999999; font-size: 10px; font-family: monospace;">
                            <b style="color: #888888;">Token usage</b><br>
                            Input: ${usage.promptTokens} | Output: ${usage.candidateTokens} | Total: ${usage.totalTokens}
                        </div>"""
                    } else ""

                    // Add a blinking cursor effect while generating
                    val cursor = if (!isFinal) "<span style='color: #888888;'>&#9608;</span>" else ""

                    // 5. Wrap it in your final layout
                    val finalHtml = """
                        <html>
                        <body style="font-family: sans-serif; padding: 10px;">
                            <h3 style="color: #888888;">&#128269; $resultHeader :</h3>
                            <hr>
                            <pre style="background-color: #2b2b2b; color: #a9b7c6; padding: 10px; white-space: pre-wrap; word-wrap: break-word;"><code>${selectedText.replace("<", "&lt;").replace(">", "&gt;")}</code></pre>
                            <hr><br>
                            <div style="line-height: 1.4;">$safeAiHtml</div>
                            $tokenHtml
                        </body>
                        </html>
                    """.trimIndent()

                    // 6. Update the UI on the EDT
                    ApplicationManager.getApplication().invokeLater {
                        if (isFinal) {
                            // Stream finished: Save it to our project state history with the extracted title
                            projectService.addToHistory(finalHtml, aiTitle)
                        } else {
                            // Still streaming: Update the text pane without saving to history
                            projectService.uiUpdater?.invoke(finalHtml)
                        }
                    }
                }

                val typewriterThread = Thread {
                    while (!isStreamFinished || charQueue.isNotEmpty()) {
                        if (charQueue.isNotEmpty()) {
                            // Dynamically adjust typing speed: type faster if a large chunk arrived
                            var charsToPull = if (charQueue.size > 200) 5 else 2
                            var changed = false

                            while (charsToPull > 0 && charQueue.isNotEmpty()) {
                                displayedMarkdown.append(charQueue.poll())
                                charsToPull--
                                changed = true
                            }

                            if (changed) {
                                // Update UI with the currently displayed markdown
                                updateUI(displayedMarkdown.toString(), isFinal = false)
                            }

                            // Delay to create the smooth typewriter effect (15ms)
                            Thread.sleep(15)
                        } else {
                            // Wait briefly for more characters to arrive from the network
                            Thread.sleep(10)
                        }
                    }
                }
                typewriterThread.start()

                // 3. Execute the network stream (Producer)
                val finalResult = geminiService.explainCodeStream(prompt) { chunk ->
                    // Instead of instantly updating the UI, offer the characters to the queue
                    for (char in chunk) {
                        charQueue.offer(char)
                    }
                }
                isStreamFinished = true
                typewriterThread.join()

                // Push the final result to UI and save to history
                updateUI(finalResult.text, isFinal = true, usage = finalResult)
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