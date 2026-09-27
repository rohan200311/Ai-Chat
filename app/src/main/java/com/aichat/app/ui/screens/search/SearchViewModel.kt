package com.aichat.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.domain.model.SearchResult
import com.aichat.app.util.SearchIntegration
import com.aichat.app.util.SearchProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val provider: String = "brave"
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchIntegration: SearchIntegration
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun setProvider(provider: String) {
        _uiState.update { it.copy(provider = provider) }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val type = when (_uiState.value.provider) {
                "tavily" -> SearchProviderType.TAVILY
                "serpapi" -> SearchProviderType.SERPAPI
                else -> SearchProviderType.BRAVE
            }
            val results = searchIntegration.search(query, type, "", 10)
            _uiState.update { it.copy(results = results, isLoading = false) }
        }
    }
}
