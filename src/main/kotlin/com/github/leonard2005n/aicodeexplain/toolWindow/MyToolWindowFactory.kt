package com.github.leonard2005n.aicodeexplain.toolWindow

import com.github.leonard2005n.aicodeexplain.services.GeminiService
import com.github.leonard2005n.aicodeexplain.services.MyProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.HyperlinkLabel
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

    // This class is responsible for creating the Tool Window, which is where the explanation will be displayed.
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {
        // Access both services: one for the explanation bridge, one for the API key storage
        private val projectService = toolWindow.project.service<MyProjectService>()
        private val geminiService = service<GeminiService>()

        fun getContent() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val mainPanel = this // Reference to the main panel for layout refreshes

            // 1. Create the Central Explanation Panel (retaining your HTML JEditorPane)
            val textArea = JEditorPane().apply {
                contentType = "text/html"
                isEditable = false
                margin = JBUI.insets(15)
                // Ensures the pane uses the IDE's default font and colors
                putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)

                // Standard IntelliJ listener to open links in the system browser
                addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
            }

            // 2. Settings Components (API Key input and management)
            val apiLabel = JBLabel("API Key:")
            val keyField = JBTextField(geminiService.getApiKey() ?: "", 20)
            val saveButton = JButton("Save")
            val editButton = JButton("Edit API Key")
            
            // Dedicated Hyperlink component for easy access to the API key site
            val apiKeyLink = HyperlinkLabel("Get API Key").apply {
                setHyperlinkTarget("https://aistudio.google.com/app/apikey")
            }

            // 3. Create a Consolidated Configuration Panel at the bottom (South)
            val settingsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.CENTER)).apply {
                // Add a top border to separate it from the explanation text
                border = JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 1, 0, 0, 0)

                add(apiLabel)
                add(keyField)
                add(saveButton)
                add(editButton)
                add(apiKeyLink)
            }

            // 4. UI Refresh Logic to handle "First Time" setup vs "Ready" state
            fun refreshUI() {
                val currentKey = geminiService.getApiKey()
                // Check if key is missing or is the default storage ID string
                val isKeyMissing = currentKey.isNullOrBlank() || currentKey == "com.github.leonard2005n.aicodeexplain.GEMINI_API_KEY"

                if (isKeyMissing) {
                    // SETUP MODE: Show inputs and guide the user to the API key site
                    textArea.text = """
                        <html>
                        <body>
                            <b>Welcome!</b><br><br>
                            To get started, please enter your 
                            <a href="https://aistudio.google.com/app/apikey">Google AI Studio API key</a> 
                            in the field at the bottom.
                        </body>
                        </html>
                    """.trimIndent()

                    // Toggle visibility: show input fields, hide edit button
                    apiLabel.isVisible = true
                    keyField.isVisible = true
                    saveButton.isVisible = true
                    editButton.isVisible = false
                    apiKeyLink.isVisible = true
                } else {
                    // READY MODE: Hide setup fields and show the default usage instructions
                    textArea.text = "<html><body>Highlight some code, right-click, and select <b>'Explain Code with AI'</b> to see the explanation here.</body></html>"

                    // Toggle visibility: hide input fields, show only edit button
                    apiLabel.isVisible = false
                    keyField.isVisible = false
                    saveButton.isVisible = false
                    editButton.isVisible = true
                    apiKeyLink.isVisible = false
                }

                // Force Swing to recalculate the layout so components physically disappear/appear
                mainPanel.revalidate()
                mainPanel.repaint()
            }

            // Button Actions
            saveButton.addActionListener {
                geminiService.setApiKey(keyField.text)
                refreshUI() // Hide the input fields immediately after saving
            }

            editButton.addActionListener {
                // Clicking edit brings back the input fields in the bottom bar for testing/changes
                val currentKey = geminiService.getApiKey() ?: ""
                keyField.text = currentKey

                // Show input components again
                apiLabel.isVisible = true
                keyField.isVisible = true
                saveButton.isVisible = true
                editButton.isVisible = false
                apiKeyLink.isVisible = true

                mainPanel.revalidate()
                mainPanel.repaint()
            }

            // Initialize the UI state based on whether a key already exists
            refreshUI()

            // Listen for updates from the ExplainCodeAction to display AI results
            projectService.uiUpdater = { newText ->
                textArea.text = newText
                textArea.caretPosition = 0
            }

            // Main Layout Assembly: Scrollable text in the center, settings at the bottom
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            add(settingsPanel, BorderLayout.SOUTH)
        }
    }
}