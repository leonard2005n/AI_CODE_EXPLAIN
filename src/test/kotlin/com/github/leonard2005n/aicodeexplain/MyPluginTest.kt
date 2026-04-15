package com.github.leonard2005n.aicodeexplain

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import com.github.leonard2005n.aicodeexplain.services.GeminiService
import com.github.leonard2005n.aicodeexplain.services.MyProjectService
import com.github.leonard2005n.aicodeexplain.services.HistoryEntry
import com.github.leonard2005n.aicodeexplain.actions.*
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
        projectService.addToHistory("Test explanation", "Test Title")
        assertEquals("Test explanation", updatedText)
    }

    fun testGeminiService() {
        val geminiService = ApplicationManager.getApplication().service<GeminiService>()
        assertNotNull("GeminiService should be registered", geminiService)

        // Test with missing API key
        geminiService.setApiKey("")
        val result = geminiService.explainCode("Test prompt")
        assertNotNull("explainCode should return a result", result)
        assertTrue("Result should contain API key missing message", result.text.contains("API key is missing"))
    }

    fun testProjectServiceHistory() {
        val projectService = project.service<MyProjectService>()
        
        projectService.addToHistory("Expl 1", "Title 1")
        projectService.addToHistory("Expl 2", "Title 2")
        projectService.addToHistory("Expl 3", "Title 3")
        
        var currentExpl = ""
        projectService.uiUpdater = { currentExpl = it }
        
        projectService.selectHistoryEntry(projectService.state.history.size - 2)
        assertEquals("Expl 2", currentExpl)
        
        projectService.selectHistoryEntry(projectService.state.history.size - 1)
        assertEquals("Expl 3", currentExpl)
        
        projectService.selectHistoryEntry(projectService.state.history.size - 3)
        assertEquals("Expl 1", currentExpl)
    }

    fun testApiKeyPersistence() {
        val geminiService = ApplicationManager.getApplication().service<GeminiService>()
        val testKey = "test-api-key-123"
        geminiService.setApiKey(testKey)
        assertEquals(testKey, geminiService.getApiKey())
        
        geminiService.setApiKey("")
        assertEquals("", geminiService.getApiKey())
    }

    fun testExplainCodeActionPrompt() {
        val action = ExplainCodeAction()
        val prompt = action.getPrompt("val x = 1", "package test\nval x = 1")
        assertTrue(prompt.contains("val x = 1"))
        assertTrue(prompt.contains("package test"))
        assertTrue(prompt.contains("Senior Software Engineer"))
    }

    fun testDebugCodeActionPrompt() {
        val action = DebugCodeAction()
        val prompt = action.getPrompt("val x = 1", "package test\nval x = 1")
        assertTrue(prompt.contains("val x = 1"))
        assertTrue(prompt.contains("Senior QA Engineer"))
    }

    fun testGenerateTestsActionPrompt() {
        val action = GenerateTestsAction()
        val prompt = action.getPrompt("val x = 1", "package test\nval x = 1")
        assertTrue(prompt.contains("val x = 1"))
        assertTrue(prompt.contains("unit test code"))
    }

    fun testRefactorCodeActionPrompt() {
        val action = RefactorCodeAction()
        val prompt = action.getPrompt("val x = 1", "package test\nval x = 1")
        assertTrue(prompt.contains("val x = 1"))
        assertTrue(prompt.contains("refactoring and optimization"))
    }

    fun testProjectServiceStatePersistence() {
        val projectService = MyProjectService(project)
        val state = MyProjectService.State()
        state.history.add(HistoryEntry("E1", "T1"))
        state.history.add(HistoryEntry("E2", "T2"))
        
        projectService.loadState(state)
        
        var currentExpl = ""
        projectService.uiUpdater = { currentExpl = it }
        projectService.refreshUI()
        
        assertEquals("E2", currentExpl)
        
        projectService.selectHistoryEntry(0)
        assertEquals("E1", currentExpl)
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
