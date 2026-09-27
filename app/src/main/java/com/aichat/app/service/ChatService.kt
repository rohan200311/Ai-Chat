package com.aichat.app.service

import com.aichat.app.data.mcp.McpClient
import com.aichat.app.data.remote.provider.ChatProvider
import com.aichat.app.domain.model.Message
import com.aichat.app.domain.model.ModelInfo
import com.aichat.app.domain.model.ProviderConfig
import com.aichat.app.util.SearchIntegration
import com.aichat.app.util.SearchProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatService @Inject constructor(
    private val mcpClient: McpClient,
    private val searchIntegration: SearchIntegration
) {
    /**
     * Orchestrates chat with:
     * - Memory injection
     * - Search augmentation
     * - MCP tools
     * - Workspace context
     */
    suspend fun streamChat(
        provider: ProviderConfig,
        model: ModelInfo,
        messages: List<Message>,
        systemPrompt: String?,
        providerClient: ChatProvider,
        searchEnabled: Boolean,
        searchApiKey: String,
        workspaceContext: String? = null
    ): Flow<com.aichat.app.data.remote.provider.ChatChunk> = flow {
        var enhancedSystem = systemPrompt ?: "You are a helpful AI assistant."

        if (workspaceContext != null) {
            enhancedSystem += "\n\n[Workspace Files]\n$workspaceContext"
        }

        if (searchEnabled && messages.lastOrNull()?.content != null) {
            val query = messages.last().content
            val results = searchIntegration.search(query, SearchProviderType.BRAVE, searchApiKey, 5)
            if (results.isNotEmpty()) {
                val searchStr = results.joinToString("\n") { "[${it.title}](${it.url}): ${it.snippet}" }
                enhancedSystem += "\n\n[Web Search Results - Use to answer accurately]\n$searchStr"
            }
        }

        val tools = mcpClient.asProviderTools()

        providerClient.chat(messages, model, provider, enhancedSystem, tools, true).collect { chunk ->
            // Intercept tool calls to execute via MCP
            if (chunk.toolCalls.isNotEmpty()) {
                for (toolCall in chunk.toolCalls) {
                    val serverId = mcpClient.tools.value.find { it.name == toolCall.name }?.serverId
                    if (serverId != null) {
                        val result = mcpClient.callTool(serverId, toolCall.name, toolCall.arguments)
                        // In real implementation, append tool result as Tool message and continue
                    }
                }
            }
            emit(chunk)
        }
    }
}
