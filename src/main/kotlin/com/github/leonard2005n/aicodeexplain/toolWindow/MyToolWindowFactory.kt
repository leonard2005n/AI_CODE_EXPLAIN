package com.github.leonard2005n.aicodeexplain.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.github.leonard2005n.aicodeexplain.services.MyProjectService
import com.intellij.openapi.diagnostic.thisLogger
import java.awt.BorderLayout


class MyToolWindowFactory : ToolWindowFactory {

    // This class is responsible for creating the Tool Window, which is where the explanation will be displayed.
    // It also listens to the MyProjectService
    // for updates to the explanation text and updates the UI accordingly.
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>();

        fun getContent() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            // Create a large text area to hold the explanation
            val textArea = JBTextArea("Welcome! Highlight some code, right-click, and select 'Explain Code with AI' to see the explanation here.").apply {
                isEditable = false // Prevent the user from typing in it
                lineWrap = true    // Make text wrap to the next line
                wrapStyleWord = true
            }

            // Tell the Service to update this text area whenever new text arrives
            service.uiUpdater = { newText ->
                textArea.text = newText
            }

            // Add the text area to a scrollable pane so it doesn't get cut off
            add(JBScrollPane(textArea), BorderLayout.CENTER)
        }
    }
}
