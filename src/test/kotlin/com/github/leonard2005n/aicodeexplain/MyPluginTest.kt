package com.github.leonard2005n.aicodeexplain

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import com.github.leonard2005n.aicodeexplain.services.GeminiService
import com.github.leonard2005n.aicodeexplain.services.MyProjectService
import com.intellij.openapi.application.ApplicationManager

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    fun testProjectService() {
        val projectService = project.service<MyProjectService>()
        var updatedText = ""
        projectService.uiUpdater = { updatedText = it }
        projectService.updateExplanation("Test update")
        assertEquals("Test update", updatedText)
    }

    fun testGeminiService() {
        val geminiService = ApplicationManager.getApplication().service<GeminiService>()
        assertNotNull("GeminiService should be registered", geminiService)
        
        val result = geminiService.explainCode("Test prompt")
        assertNotNull("explainCode should return a result", result)
        assertTrue("Result should be a string", result is String)
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
