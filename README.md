# AI_CODE_EXPLAIN

![Build](https://github.com/leonard2005n/AI_CODE_EXPLAIN/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.github.leonard2005n.aicodeexplain.svg)](https://plugins.jetbrains.com/plugin/com.github.leonard2005n.aicodeexplain)

<!-- Plugin description -->
**AI_CODE_EXPLAIN** is a powerful IntelliJ Platform Plugin that brings advanced AI capabilities directly into your development workflow. Powered by Google's Gemini API (using the `gemma-4-31b-it` model), it offers a suite of tools to help you understand, debug, and improve your code.

### Core Features
- **Explain Code**: Get a high-level summary and step-by-step breakdown of any code snippet, including its context within the file.
- **Find Bugs**: Automatically identify potential bugs, logical errors, and edge cases in your selected code.
- **Refactor Code**: Receive AI-driven suggestions to simplify, clean up, and optimize your implementation.
- **Generate Tests**: Instantly create unit test templates for your functions and classes to improve code coverage.
- **Real-time Streaming**: Watch as the AI generates its response in real-time, providing immediate feedback.
- **History Management**: Browse through your previous AI interactions using a dedicated history dropdown, persisted across IDE restarts.
- **Token Usage Transparency**: Keep track of your API usage with detailed token counts (Input/Output/Total) for every request.
- **Rich Formatting**: Beautifully rendered explanations using Markdown with syntax-highlighted code blocks.

<!-- Plugin description end -->

## Getting Started

1. **Install the Plugin**: Install via the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/com.github.leonard2005n.aicodeexplain) or from the IDE's plugin settings.
2. **Set Your API Key**:
    - Open the **"AI Explainer"** tool window (usually on the right sidebar).
    - Click the **"Get API Key"** link to obtain a free key from [Google AI Studio](https://aistudio.google.com/app/apikey).
    - Enter your key in the settings field at the bottom and click **"Save"**.
3. **Use AI Tools**:
    - Highlight any code in your editor.
    - Right-click to open the context menu.
    - Navigate to **"AI Code Tools"** and select an action: *Explain Code*, *Find Bugs*, *Refactor Code*, or *Generate Tests*.
    - View the results in the **"AI Explainer"** tool window.

## Installation

- **Using the IDE built-in plugin system**:
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "AI_CODE_EXPLAIN"</kbd> > <kbd>Install</kbd>

- **Manual Installation**:
  Download the [latest release](https://github.com/leonard2005n/AI_CODE_EXPLAIN/releases/latest) and install it via <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Built with Kotlin and the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
