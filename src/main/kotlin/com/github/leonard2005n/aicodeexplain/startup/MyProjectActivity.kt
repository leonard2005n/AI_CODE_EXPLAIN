package com.github.leonard2005n.aicodeexplain.startup

import com.github.leonard2005n.aicodeexplain.services.GeminiService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.Messages

class MyProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {

    }
}