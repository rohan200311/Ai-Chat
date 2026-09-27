package com.aichat.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class Role { USER, ASSISTANT, SYSTEM, TOOL }

enum class ProviderType {
    OPENAI, ANTHROPIC, GOOGLE, GROQ, MISTRAL, OLLAMA, OPENROUTER, CUSTOM
}

@Serializable
data class ProviderConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ProviderType,
    val baseUrl: String,
    val apiKey: String = "",
    val customHeaders: Map<String, String> = emptyMap(),
    val enabledModels: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val supportsVision: Boolean = true,
    val supportsTools: Boolean = true,
    val supportsStreaming: Boolean = true
)

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val providerId: String,
    val contextLength: Int = 8192,
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = false,
    val supportsReasoning: Boolean = false,
    val inputPricePer1M: Double? = null,
    val outputPricePer1M: Double? = null,
    val description: String = ""
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val providerId: String,
    val modelId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val systemPrompt: String? = null,
    val memoryEnabled: Boolean = true,
    val workspaceId: String? = null,
    val pinned: Boolean = false,
    val folder: String? = null
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val parentId: String? = null, // for branching
    val role: Role,
    val content: String,
    val reasoning: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val tokensUsed: Int? = null,
    val attachments: List<Attachment> = emptyList(),
    val toolCalls: List<ToolCall> = emptyList(),
    val isStreaming: Boolean = false,
    val branchChildren: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val type: AttachmentType,
    val name: String,
    val mimeType: String,
    val uri: String? = null,
    val base64Data: String? = null,
    val extractedText: String? = null,
    val size: Long = 0
)

enum class AttachmentType { IMAGE, PDF, DOCX, TEXT, AUDIO, OTHER }

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String,
    val result: String? = null
)

data class BranchNode(
    val messageId: String,
    val children: List<BranchNode> = emptyList()
)

data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String? = null, // null = global memory
    val key: String,
    val value: String,
    val importance: Float = 0.5f,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis()
)

data class PromptTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val template: String, // contains {{variable}}
    val variables: List<PromptVariable> = emptyList(),
    val description: String = ""
)

data class PromptVariable(
    val name: String,
    val description: String = "",
    val defaultValue: String = "",
    val required: Boolean = false
)

data class WorkspaceFile(
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val name: String,
    val path: String,
    val content: String,
    val language: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Workspace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val files: List<WorkspaceFile> = emptyList(),
    val isAgentMode: Boolean = false
)

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String = "web"
)
