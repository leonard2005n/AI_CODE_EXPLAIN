# AI_CODE_EXPLAIN

![Build](https://github.com/leonard2005n/AI_CODE_EXPLAIN/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.github.leonard2005n.aicodeexplain.svg)](https://plugins.jetbrains.com/plugin/com.github.leonard2005n.aicodeexplain)

<!-- Plugin description -->
**AI_CODE_EXPLAIN** is an IntelliJ Platform Plugin designed to help developers understand complex code snippets quickly. By leveraging the power of Google's Gemini (specifically the `gemma-4-31b-it` model), it provides clear, structured explanations of selected code directly within your IDE.

### Features
- **Instant AI Explanations**: Select any piece of code and get a detailed breakdown in seconds.
- **Context-Aware Analysis**: The AI uses the full file context to provide more accurate and relevant explanations.
- **Dedicated Tool Window**: View explanations in a clean, formatted tool window without losing your place in the code.
- **Asynchronous Execution**: Requests are handled in the background, ensuring your IDE remains smooth and responsive.
- **Formatted Output**: Enjoy easy-to-read explanations with high-level summaries and step-by-step breakdowns.

<!-- Plugin description end -->

## How to Use

1. **Select Code**: Highlight the code snippet you want to understand in any editor window.
2. **Trigger Action**: Right-click on the selected text and choose **"Explain Code with AI"** from the context menu.
3. **View Explanation**: The **"MyToolWindow"** will open automatically on the side, displaying the AI-generated explanation.

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "AI_CODE_EXPLAIN"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/leonard2005n/AI_CODE_EXPLAIN/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
