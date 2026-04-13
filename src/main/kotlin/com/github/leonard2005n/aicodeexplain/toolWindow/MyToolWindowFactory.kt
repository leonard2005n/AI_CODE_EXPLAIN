package com.github.leonard2005n.aicodeexplain.toolWindow

import com.github.leonard2005n.aicodeexplain.services.GeminiService
import com.github.leonard2005n.aicodeexplain.services.HistoryEntry
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
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import javax.swing.text.html.HTMLEditorKit
import com.intellij.openapi.ui.ComboBox
import java.awt.event.ItemEvent

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

        // This method constructs the entire UI of the tool window, including the explanation display area,
        // navigation buttons, and API key management components.
        fun getContent() = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val mainPanel = this // Reference to the main panel for layout refreshes

            // 1. Create the Central Explanation Panel (retaining your HTML JEditorPane)
            val textArea = JEditorPane().apply {
                contentType = "text/html"
                isEditable = false
                margin = JBUI.insets(15)
                putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
                addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)

                // Inject safe, IDE-friendly CSS rules globally
                val kit = HTMLEditorKit()
                val styleSheet = kit.styleSheet
                styleSheet.addRule("body { font-family: sans-serif; font-size: 13pt; line-height: 1.4; }")
                styleSheet.addRule("h3 { color: #888888; margin-top: 15px; }")
                styleSheet.addRule("pre { background-color: #2b2b2b; color: #a9b7c6; padding: 10px; border: 1px solid #555555; }")
                styleSheet.addRule("code { font-family: monospace; background-color: #2b2b2b; color: #a9b7c6; padding: 2px 4px; }")
                styleSheet.addRule("ul { margin-left: 15px; }")
                styleSheet.addRule("li { margin-bottom: 5px; }")
                styleSheet.addRule("pre { background-color: #2b2b2b; color: #a9b7c6; padding: 10px; border: 1px solid #555555; white-space: pre-wrap; word-wrap: break-word; }")
                styleSheet.addRule("code { font-family: monospace; background-color: #2b2b2b; color: #a9b7c6; padding: 2px 4px; word-wrap: break-word; }")
                editorKit = kit
            }

            // Flag to track whether we are actively streaming text
            var isGenerating = false

            // Creating the histroy drop down
            val historyDropdown = ComboBox<HistoryEntry>().apply {
                isSwingPopup = false
            }

            var removeButton = JButton("remove").apply {isEnabled = false}


            val navPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(historyDropdown, BorderLayout.CENTER)
                add(removeButton, BorderLayout.EAST)
                border = JBUI.Borders.customLine(JBUI.CurrentTheme.ToolWindow.borderColor(), 0, 0, 1, 0)
            }

            // Button Actions for navigation
            historyDropdown.addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    val selectedIndex = historyDropdown.selectedIndex
                    if (selectedIndex != -1) {
                        projectService.selectHistoryEntry(selectedIndex)
                    }
                }
            }
            removeButton.addActionListener { projectService.removeFromHistory() }

            projectService.dropdownListUpdater = { historyList, selectedIndex ->
                // Temporarily remove listener to avoid infinite loops while updating model
                val listeners = historyDropdown.itemListeners
                listeners.forEach { historyDropdown.removeItemListener(it) }

                historyDropdown.removeAllItems()
                historyList.forEach { historyDropdown.addItem(it) }

                if (selectedIndex >= 0 && selectedIndex < historyDropdown.itemCount) {
                    historyDropdown.selectedIndex = selectedIndex
                }

                removeButton.isEnabled = historyList.isNotEmpty()

                // Re-add listener
                listeners.forEach { historyDropdown.addItemListener(it) }
            }

            // 3. Settings Components (API Key input and management)
            val apiLabel = JBLabel("API Key:")
            val keyField = JBTextField(geminiService.getApiKey() ?: "", 20)
            val saveButton = JButton("Save")
            val editButton = JButton("Edit API Key")


            // 2. Add the progress bar right after creating navPanel
            val progressBar = JProgressBar().apply {
                isIndeterminate = true // Makes the bar slide back and forth automatically
                isVisible = false      // Hidden by default
            }

            // Create a top panel to hold both the navigation and the progress bar together
            val topPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(navPanel, BorderLayout.CENTER)
                add(progressBar, BorderLayout.SOUTH)
            }

            // Update the loading state from the service to show/hide the progress bar and display a temporary message
            projectService.loadingStateUpdater = { isLoading ->
                isGenerating = isLoading // Update our local tracking flag
                progressBar.isVisible = isLoading
                historyDropdown.isEnabled = !isLoading // Disable dropdown while loading
                removeButton.isEnabled = !isLoading && historyDropdown.itemCount > 0

                if (isLoading) {
                    textArea.text = "<html><body><h3 style='color: #888888;'>&#8987; Generating response...</h3></body></html>"
                } else {
                    projectService.refreshUI()
                }
            }

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
                    textArea.text = "<html><body>Highlight some code, right-click, and select an AI action to see the explanation here.</body></html>"

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

                // Use invokeLater to wait for the HTML parser to finish rendering the layout
                SwingUtilities.invokeLater {
                    try {
                        if (isGenerating) {
                            // If actively streaming, scroll to follow the new text at the bottom
                            textArea.caretPosition = textArea.document.length
                        } else {
                            // If user selected history, reset to top so they can read it
                            textArea.caretPosition = 0
                        }
                    } catch (e: Exception) {
                        // Document might not be fully parsed yet, safely ignore
                    }
                }
            }

            // Main Layout Assembly: Navigation at top, Scrollable text in the center, settings at the bottom
            add(topPanel, BorderLayout.NORTH)
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            add(settingsPanel, BorderLayout.SOUTH)

            // Load any existing history from the previous session (if available)
            // This will overwrite the default "Highlight some code..." text if they have saved history.
            projectService.refreshUI()
        }
    }
}