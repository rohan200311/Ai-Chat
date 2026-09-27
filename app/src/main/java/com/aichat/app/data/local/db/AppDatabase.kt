package com.aichat.app.data.local.db

import androidx.room.*
import com.aichat.app.data.local.db.dao.*
import com.aichat.app.domain.model.*
import kotlinx.serialization.Serializable

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val providerId: String,
    val modelId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val systemPrompt: String?,
    val memoryEnabled: Boolean,
    val workspaceId: String?,
    val pinned: Boolean,
    val folder: String?
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val parentId: String?,
    val role: Role,
    val content: String,
    val reasoning: String?,
    val createdAt: Long,
    val tokensUsed: Int?,
    val attachments: List<Attachment>,
    val toolCalls: List<ToolCall>,
    val branchChildren: List<String>,
    val metadata: Map<String, String>
)

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val customHeaders: Map<String, String>,
    val enabledModels: List<String>,
    val isEnabled: Boolean,
    val createdAt: Long,
    val supportsVision: Boolean,
    val supportsTools: Boolean,
    val supportsStreaming: Boolean
)

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val providerId: String,
    val contextLength: Int,
    val supportsVision: Boolean,
    val supportsTools: Boolean,
    val supportsReasoning: Boolean,
    val inputPricePer1M: Double?,
    val outputPricePer1M: Double?,
    val description: String
)

@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val conversationId: String?,
    val key: String,
    val value: String,
    val importance: Float,
    val createdAt: Long,
    val lastAccessed: Long
)

@Entity(tableName = "prompt_templates")
data class PromptTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val template: String,
    val description: String
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val isAgentMode: Boolean
)

@Entity(tableName = "workspace_files")
data class WorkspaceFileEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val path: String,
    val content: String,
    val language: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ProviderEntity::class,
        ModelEntity::class,
        MemoryEntity::class,
        PromptTemplateEntity::class,
        WorkspaceEntity::class,
        WorkspaceFileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun providerDao(): ProviderDao
    abstract fun modelDao(): ModelDao
    abstract fun memoryDao(): MemoryDao
    abstract fun workspaceDao(): WorkspaceDao
}
