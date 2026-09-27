package com.aichat.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.repository.ChatRepository
import com.aichat.app.data.repository.ProviderRepository
import com.aichat.app.domain.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.observeConversations().collect { convs ->
                _uiState.update { it.copy(conversations = convs, isLoading = false) }
            }
        }
    }

    fun createNewChat(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val providers = providerRepository.getAll()
            val defaultProvider = providers.firstOrNull() ?: run {
                // Create demo provider placeholder
                return@launch
            }
            val conv = chatRepository.createConversation(
                providerId = defaultProvider.id,
                modelId = defaultProvider.enabledModels.firstOrNull() ?: "gpt-4o-mini",
                title = "New Chat"
            )
            onCreated(conv.id)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
        }
    }
}
