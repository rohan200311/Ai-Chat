package com.aichat.app.ui.screens.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.repository.ProviderRepository
import com.aichat.app.domain.model.ModelInfo
import com.aichat.app.domain.model.ProviderConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelPickerUiState(
    val providers: List<ProviderConfig> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    val selectedProviderId: String? = null
)

@HiltViewModel
class ModelPickerViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ModelPickerUiState())
    val uiState: StateFlow<ModelPickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            providerRepository.observeProviders().collect { providers ->
                _uiState.update { it.copy(providers = providers, selectedProviderId = it.selectedProviderId ?: providers.firstOrNull()?.id) }
                _uiState.value.selectedProviderId?.let { selectProvider(it) }
            }
        }
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch {
            providerRepository.observeModels(providerId).collect { models ->
                _uiState.update { it.copy(models = models, selectedProviderId = providerId) }
            }
        }
    }
}
