package com.github.leonard2005n.aicodeexplain.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    // This is a callback function. The Tool Window will listen to this.
    var uiUpdater: ((String) -> Unit)? = null

    // The Action will call this function to send the AI text to the Tool Window
    fun updateExplanation(text: String) {
        uiUpdater?.invoke(text)
    }
}