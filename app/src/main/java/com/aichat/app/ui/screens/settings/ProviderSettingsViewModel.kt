package com.aichat.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.repository.ProviderRepository
import com.aichat.app.domain.model.ProviderConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderSettingsUiState(
    val providers: List<ProviderConfig> = emptyList(),
    val exportJson: String = ""
)

@HiltViewModel
class ProviderSettingsViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderSettingsUiState())
    val uiState: StateFlow<ProviderSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            providerRepository.observeProviders().collect { list ->
                _uiState.update { it.copy(providers = list) }
            }
        }
    }

    fun addProvider(config: ProviderConfig) {
        viewModelScope.launch { providerRepository.upsertProvider(config) }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch { providerRepository.deleteProvider(id) }
    }

    fun exportProvider(provider: ProviderConfig) {
        viewModelScope.launch {
            val json = providerRepository.exportProviders()
            _uiState.update { it.copy(exportJson = json) }
        }
    }

    fun importProviders(json: String) {
        viewModelScope.launch { providerRepository.importProviders(json) }
    }
}
