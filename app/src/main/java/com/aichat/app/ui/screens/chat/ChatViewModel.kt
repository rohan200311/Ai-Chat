package com.aichat.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.mcp.McpClient
import com.aichat.app.data.repository.ChatRepository
import com.aichat.app.data.repository.MemoryRepository
import com.aichat.app.data.repository.ProviderRepository
import com.aichat.app.domain.model.*
import com.aichat.app.util.FileParser
import com.aichat.app.util.PromptVariableParser
import com.aichat.app.util.SearchIntegration
import com.aichat.app.util.SearchProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val streamingContent: String = "",
    val streamingReasoning: String = "",
    val providers: List<ProviderConfig> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    val selectedModel: ModelInfo? = null,
    val attachments: List<Attachment> = emptyList(),
    val promptVariables: Map<String, String> = emptyMap(),
    val showModelPicker: Boolean = false,
    val showBranchDialog: Boolean = false,
    val branchSourceMessageId: String? = null,
    val searchEnabled: Boolean = false,
    val memoryContext: List<MemoryEntry> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val memoryRepository: MemoryRepository,
    private val fileParser: FileParser,
    private val searchIntegration: SearchIntegration,
    private val mcpClient: McpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var conversationId: String = ""

    fun loadConversation(id: String) {
        conversationId = id
        viewModelScope.launch {
            // Observe conversation
            val conv = chatRepository.getConversation(id)
            _uiState.update { it.copy(conversation = conv) }

            // Observe messages
            chatRepository.observeMessages(id).collect { messages ->
                _uiState.update { state -> state.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            providerRepository.observeProviders().collect { providers ->
                _uiState.update { it.copy(providers = providers) }
                // Load models for current provider
                val providerId = _uiState.value.conversation?.providerId ?: providers.firstOrNull()?.id
                if (providerId != null) {
                    providerRepository.observeModels(providerId).collect { models ->
                        _uiState.update { s -> s.copy(models = models, selectedModel = models.find { m -> m.id == s.conversation?.modelId }) }
                    }
                }
            }
        }
        viewModelScope.launch {
            val memory = memoryRepository.getRelevant(id)
            _uiState.update { it.copy(memoryContext = memory) }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() && _uiState.value.attachments.isEmpty()) return
        viewModelScope.launch {
            val conv = _uiState.value.conversation ?: return@launch
            val providers = _uiState.value.providers
            val provider = providers.find { it.id == conv.providerId } ?: return@launch
            val model = _uiState.value.selectedModel ?: return@launch

            // Handle prompt variables
            val variables = PromptVariableParser.extractVariables(content)
            val renderedContent = if (variables.isNotEmpty()) {
                PromptVariableParser.render(content, _uiState.value.promptVariables)
            } else content

            // Create user message
            val parentId = _uiState.value.messages.lastOrNull()?.id
            val userMessage = Message(
                conversationId = conversationId,
                parentId = parentId,
                role = Role.USER,
                content = renderedContent,
                attachments = _uiState.value.attachments
            )
            chatRepository.addMessage(userMessage)
            _uiState.update { it.copy(attachments = emptyList(), isLoading = true, streamingContent = "") }

            // Build context: system + memory + search + messages
            var systemPrompt = conv.systemPrompt ?: "You are a helpful AI assistant."
            if (_uiState.value.memoryContext.isNotEmpty()) {
                val memStr = _uiState.value.memoryContext.joinToString("\n") { "${it.key}: ${it.value}" }
                systemPrompt += "\n\n[Memory]\n$memStr"
            }

            // Search integration
            var searchContext = ""
            if (_uiState.value.searchEnabled) {
                val results = searchIntegration.search(renderedContent, SearchProviderType.BRAVE, "", 5)
                if (results.isNotEmpty()) {
                    searchContext = results.joinToString("\n") { "[${it.title}](${it.url}): ${it.snippet}" }
                    systemPrompt += "\n\n[Web Search Results]\n$searchContext"
                }
            }

            // MCP tools
            val mcpTools = mcpClient.asProviderTools()

            // Stream response
            val providerClient = chatRepository.buildChatProvider(provider)
            val history = chatRepository.getBranch(conversationId, userMessage.id)

            try {
                val assistantMessageId = java.util.UUID.randomUUID().toString()
                var fullContent = ""
                var fullReasoning = ""
                val toolCalls = mutableListOf<ToolCall>()

                providerClient.chat(
                    messages = history,
                    model = model,
                    provider = provider,
                    systemPrompt = systemPrompt,
                    tools = mcpTools,
                    stream = true
                ).collect { chunk ->
                    fullContent += chunk.content
                    if (chunk.reasoning != null) fullReasoning += chunk.reasoning
                    if (chunk.toolCalls.isNotEmpty()) toolCalls.addAll(chunk.toolCalls)

                    _uiState.update {
                        it.copy(
                            streamingContent = fullContent,
                            streamingReasoning = fullReasoning
                        )
                    }

                    if (chunk.isDone) {
                        val assistantMessage = Message(
                            id = assistantMessageId,
                            conversationId = conversationId,
                            parentId = userMessage.id,
                            role = Role.ASSISTANT,
                            content = fullContent,
                            reasoning = fullReasoning.ifBlank { null },
                            toolCalls = toolCalls
                        )
                        chatRepository.addMessage(assistantMessage)
                        _uiState.update { it.copy(isLoading = false, streamingContent = "", streamingReasoning = "") }

                        // Auto-title generation
                        if (history.size <= 2) {
                            generateTitleIfNeeded()
                        }

                        // Save memory summary periodically
                        if (history.size % 10 == 0) {
                            memoryRepository.summarizeConversation(history + userMessage + assistantMessage)?.let {
                                memoryRepository.save(it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun addAttachment(uri: android.net.Uri) {
        viewModelScope.launch {
            val attachment = fileParser.parseUri(uri)
            _uiState.update { it.copy(attachments = it.attachments + attachment) }
        }
    }

    fun removeAttachment(id: String) {
        _uiState.update { it.copy(attachments = it.attachments.filterNot { a -> a.id == id }) }
    }

    fun toggleModelPicker(show: Boolean) {
        _uiState.update { it.copy(showModelPicker = show) }
    }

    fun selectModel(model: ModelInfo) {
        viewModelScope.launch {
            val conv = _uiState.value.conversation ?: return@launch
            // Update conversation with new model
            val updated = conv.copy(modelId = model.id, providerId = model.providerId)
            // Save via repository (upsert)
            // For simplicity, direct DB update via repository method would be needed
            _uiState.update { it.copy(selectedModel = model, conversation = updated, showModelPicker = false) }
        }
    }

    fun branchFromMessage(messageId: String) {
        _uiState.update { it.copy(showBranchDialog = true, branchSourceMessageId = messageId) }
    }

    fun createBranch(newContent: String) {
        viewModelScope.launch {
            val sourceId = _uiState.value.branchSourceMessageId ?: return@launch
            chatRepository.createBranch(sourceId, newContent)
            _uiState.update { it.copy(showBranchDialog = false, branchSourceMessageId = null) }
        }
    }

    fun regenerateLast() {
        val lastUser = _uiState.value.messages.lastOrNull { it.role == Role.USER } ?: return
        sendMessage(lastUser.content)
    }

    fun setPromptVariable(name: String, value: String) {
        _uiState.update { it.copy(promptVariables = it.promptVariables + (name to value)) }
    }

    fun toggleSearch(enabled: Boolean) {
        _uiState.update { it.copy(searchEnabled = enabled) }
    }

    private suspend fun generateTitleIfNeeded() {
        // Simplified: use first user message as title
        val firstUser = _uiState.value.messages.firstOrNull { it.role == Role.USER }?.content?.take(50)
        if (!firstUser.isNullOrBlank()) {
            chatRepository.updateConversationTitle(conversationId, firstUser)
        }
    }
}
