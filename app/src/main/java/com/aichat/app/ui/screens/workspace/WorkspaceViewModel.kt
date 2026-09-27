package com.aichat.app.ui.screens.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.domain.model.WorkspaceFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceUiState(
    val files: List<WorkspaceFile> = emptyList(),
    val isAgentMode: Boolean = false,
    val agentPrompt: String = "",
    val logs: List<String> = emptyList()
)

@HiltViewModel
class WorkspaceViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    init {
        // Load workspace files from DB (simplified)
        _uiState.update {
            it.copy(files = listOf(
                WorkspaceFile(workspaceId = "default", name = "main.py", path = "/main.py", content = "print('Hello')", language = "python"),
                WorkspaceFile(workspaceId = "default", name = "README.md", path = "/README.md", content = "# Workspace", language = "markdown")
            ))
        }
    }

    fun toggleAgentMode() {
        _uiState.update { it.copy(isAgentMode = !it.isAgentMode) }
    }

    fun createFile() {
        viewModelScope.launch {
            val newFile = WorkspaceFile(workspaceId = "default", name = "file_${System.currentTimeMillis()}.txt", path = "/file.txt", content = "")
            _uiState.update { it.copy(files = it.files + newFile) }
        }
    }

    fun updateAgentPrompt(prompt: String) {
        _uiState.update { it.copy(agentPrompt = prompt) }
    }

    fun runAgent() {
        viewModelScope.launch {
            _uiState.update { it.copy(logs = it.logs + "Agent started: ${_uiState.value.agentPrompt}") }
            // In real app: call LLM with workspace tools via MCP
            // Tools: read_file, write_file, execute_code, search
            _uiState.update { it.copy(logs = it.logs + "Agent analyzing task...") }
            _uiState.update { it.copy(logs = it.logs + "Agent completed (mock)") }
        }
    }
}
