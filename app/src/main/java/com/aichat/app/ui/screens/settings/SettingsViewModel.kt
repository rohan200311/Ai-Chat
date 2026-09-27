package com.aichat.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "system",
    val dynamicColor: Boolean = true,
    val streamingEnabled: Boolean = true,
    val memoryEnabled: Boolean = true,
    val mcpEnabled: Boolean = false,
    val autoTitle: Boolean = true,
    val fontScale: Float = 1f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.theme,
                settingsDataStore.dynamicColor,
                settingsDataStore.streamingEnabled,
                settingsDataStore.memoryEnabled,
                settingsDataStore.mcpEnabled,
                settingsDataStore.fontScale
            ) { theme, dynamic, streaming, memory, mcp, fontScale ->
                SettingsUiState(theme, dynamic, streaming, memory, mcp, true, fontScale)
            }.collect { _uiState.value = it }
        }
    }

    fun setTheme(theme: String) = viewModelScope.launch { settingsDataStore.setTheme(theme) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setDynamicColor(enabled) }
    fun setMcpEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setMcpEnabled(enabled) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsDataStore.setFontScale(scale) }
    fun setAutoTitle(enabled: Boolean) = viewModelScope.launch { /* datastore */ }
}
