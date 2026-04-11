package com.github.leonard2005n.aicodeexplain.toolWindow

import com.github.leonard2005n.aicodeexplain.services.GeminiService
import com.github.leonard2005n.aicodeexplain.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JEditorPane

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {
        private val projectService = toolWindow.project.service<MyProjectService>()
        private val geminiService = service<GeminiService>()

        fun getContent() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val mainPanel = this

            // 1. Central Explanation Area
            val textArea = JEditorPane().apply {
                contentType = "text/html"
                isEditable = false
                margin = JBUI.insets(15)
            }

            // 2. Settings Components (Now defined together)
            val keyField = JBTextField(geminiService.getApiKey() ?: "", 20)
            val saveButton = JButton("Save")
            val editButton = JButton("Edit API Key")

            // 3. The Bottom Panel (Consolidating everything here)
            val settingsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.CENTER)).apply {
                // Add a top border to separate settings from the explanation text
                border = JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 1, 0, 0, 0)

                // Add all potential components (we will toggle visibility)
                add(JBLabel("API Key:"))
                add(keyField)
                add(saveButton)
                add(editButton)
            }

            // 4. UI Refresh Logic
            fun refreshUI() {
                val currentKey = geminiService.getApiKey()
                // Checks if key is missing or is the default placeholder string
                val isKeyMissing = currentKey.isNullOrBlank() || currentKey == "com.github.leonard2005n.aicodeexplain.GEMINI_API_KEY"

                if (isKeyMissing) {
                    // SETUP MODE: Show input fields, hide edit button
                    textArea.text = "<html><body><b>Welcome!</b><br><br>Please enter your Google AI Studio API key at the bottom to get started.</body></html>"

                    // Show input components
                    settingsPanel.components[0].isVisible = true // Label
                    keyField.isVisible = true
                    saveButton.isVisible = true
                    editButton.isVisible = false
                } else {
                    // READY MODE: Hide input fields, show only edit button
                    textArea.text = "<html><body>Highlight some code, right-click, and select <b>'Explain Code with AI'</b> to see the explanation here.</body></html>"

                    // Hide input components
                    settingsPanel.components[0].isVisible = false // Label
                    keyField.isVisible = false
                    saveButton.isVisible = false
                    editButton.isVisible = true
                }

                mainPanel.revalidate()
                mainPanel.repaint()
            }

            // Button Actions
            saveButton.addActionListener {
                geminiService.setApiKey(keyField.text)
                refreshUI()
            }

            editButton.addActionListener {
                // Clicking edit brings back the input fields in the bottom bar
                val currentKey = geminiService.getApiKey() ?: ""
                keyField.text = currentKey

                // Show input components again
                settingsPanel.components[0].isVisible = true
                keyField.isVisible = true
                saveButton.isVisible = true
                editButton.isVisible = false

                mainPanel.revalidate()
                mainPanel.repaint()
            }

            // Initialize UI state
            refreshUI()

            // Update text area when AI explanation arrives
            projectService.uiUpdater = { newText ->
                textArea.text = newText
                textArea.caretPosition = 0
            }

            // Main Layout Assembly
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            add(settingsPanel, BorderLayout.SOUTH) // Everything settings-related is now at the bottom
        }
    }
}