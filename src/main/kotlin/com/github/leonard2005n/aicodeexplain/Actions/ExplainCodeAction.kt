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

        if (selectedText.isNullOrBlank()) {
            return
        }

        // Run the explanation in a background task to avoid freezing the UI
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Explaining Code...") {
            override fun run(indicator: ProgressIndicator) {
                // 2. Call the GeminiService to get the explanation
                val geminiService = ApplicationManager.getApplication().service<GeminiService>()
                val explanation = geminiService.explainCode(selectedText, fileText)

                // 3. Show the explanation in the Tool Window instead of a dialog
                ApplicationManager.getApplication().invokeLater {
                    // Send the formatted text to the Project Service bridge
                    val projectService = project.service<MyProjectService>()
                    projectService.updateExplanation("=== Code Analyzed ===\n$selectedText\n\n=== AI Explanation ===\n$explanation")

                    // Automatically slide open the Tool Window so the user sees it
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("MyToolWindow")
                    toolWindow?.show()
                }
            }
        })
    }

    // 5. This ensures the button is ONLY clickable if text is highlighted
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor?.selectionModel?.hasSelection() == true
    }
}