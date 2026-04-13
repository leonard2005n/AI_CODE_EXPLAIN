package com.github.leonard2005n.aicodeexplain.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project


// Create a data class for the History Item
data class HistoryEntry(var explanation: String = "", var title: String = "") {
    override fun toString(): String {
        return title;
    }
}

// This service will be project-level, meaning each project will have
// its own instance of this service and its own persisted state.
@Service(Service.Level.PROJECT)
@State(
    name = "AICodeExplainHistory",
    storages = [Storage("AICodeExplainHistoryV2.xml")] // This file will be saved in the .idea folder
)
class MyProjectService(project: Project) : PersistentStateComponent<MyProjectService.State> {

    // This is the state that will be persisted
    class State {
        var history: MutableList<HistoryEntry> = mutableListOf()
    }

    // Callbacks for the UI
    var uiUpdater: ((String) -> Unit)? = null
    var navStateUpdater: ((Boolean, Boolean, Boolean) -> Unit)? = null // Updates Back/Forward button states
    var loadingStateUpdater: ((Boolean) -> Unit)? = null
    var dropdownListUpdater: ((List<HistoryEntry>, Int) -> Unit)? = null

    private var myState = State()
    private var currentIndex = -1

    // 2. PersistentStateComponent required methods
    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
        currentIndex = myState.history.size - 1 // Set to last item
    }

    // 3. Methods to manage history
    fun addToHistory(explanation: String ,title: String) {
        myState.history.add(HistoryEntry(explanation, title))

        // Optional: Limit history size to 50 to prevent the XML file from getting too large
        if (myState.history.size > 50) {
            myState.history.removeAt(0)
        }

        currentIndex = myState.history.size - 1
        refreshUI()
    }

    fun selectHistoryEntry(index: Int) {
        if (index in myState.history.indices) {
            currentIndex = index
            refreshUI()
        }
    }

    // 4. Navigation methods
    fun goBack() {
        if (canGoBack()) {
            currentIndex--
            refreshUI()
        }
    }

    fun goForward() {
        if (canGoForward()) {
            currentIndex++
            refreshUI()
        }
    }

    // Remove a specific entry from history (optional, for future use)
    fun removeFromHistory() {
         myState.history.removeAt(currentIndex)
         if (currentIndex >= myState.history.size) {
             currentIndex = myState.history.size - 1
         }

        refreshUI()
    }


    private fun canDelete() = currentIndex >= 0
    private fun canGoBack() = currentIndex > 0
    private fun canGoForward() = currentIndex < myState.history.size - 1

    fun refreshUI() {
        if (currentIndex in 0 until myState.history.size) {
            uiUpdater?.invoke(myState.history[currentIndex].explanation)
        } else {
            uiUpdater?.invoke("<html><body>Highlight some code, right-click, and select an AI action to see the explanation here.</body></html>")
        }

        // Push the updated list to the Dropdown
        dropdownListUpdater?.invoke(myState.history, currentIndex)
    }

    // Add this function anywhere inside the class:
    fun setLoading(isLoading: Boolean) {
        loadingStateUpdater?.invoke(isLoading)
    }
}