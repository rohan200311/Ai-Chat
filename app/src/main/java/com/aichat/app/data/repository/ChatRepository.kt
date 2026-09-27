package com.aichat.app.data.repository

import com.aichat.app.data.local.db.AppDatabase
import com.aichat.app.data.local.db.ConversationEntity
import com.aichat.app.data.local.db.MessageEntity
import com.aichat.app.data.local.db.ProviderEntity
import com.aichat.app.data.remote.provider.ChatProvider
import com.aichat.app.data.remote.provider.ProviderFactory
import com.aichat.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val db: AppDatabase,
    private val providerRepository: ProviderRepository,
    private val memoryRepository: MemoryRepository
) {
    fun observeConversations(): Flow<List<Conversation>> =
        db.conversationDao().observeAll().map { list -> list.map { it.toDomain() } }

    fun observeMessages(conversationId: String): Flow<List<Message>> =
        db.messageDao().observeByConversation(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun getConversation(id: String): Conversation? = db.conversationDao().getById(id)?.toDomain()

    suspend fun createConversation(
        providerId: String,
        modelId: String,
        systemPrompt: String? = null,
        title: String = "New Chat"
    ): Conversation {
        val conv = Conversation(
            title = title,
            providerId = providerId,
            modelId = modelId,
            systemPrompt = systemPrompt
        )
        db.conversationDao().upsert(conv.toEntity())
        return conv
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        db.conversationDao().updateTitle(id, title)
    }

    suspend fun deleteConversation(id: String) {
        db.messageDao().deleteByConversation(id)
        db.conversationDao().delete(id)
    }

    suspend fun addMessage(message: Message) {
        db.messageDao().upsert(message.toEntity())
        // update conversation timestamp
        db.conversationDao().getById(message.conversationId)?.let {
            db.conversationDao().upsert(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun getMessages(conversationId: String): List<Message> =
        db.messageDao().getByConversation(conversationId).map { it.toDomain() }

    suspend fun getBranch(conversationId: String, leafMessageId: String): List<Message> {
        // Walk parent chain to build current branch
        val all = getMessages(conversationId).associateBy { it.id }
        val branch = mutableListOf<Message>()
        var currentId: String? = leafMessageId
        while (currentId != null) {
            val msg = all[currentId] ?: break
            branch.add(0, msg)
            currentId = msg.parentId
        }
        return branch
    }

    suspend fun createBranch(fromMessageId: String, newContent: String): Message {
        val parent = db.messageDao().getById(fromMessageId)?.toDomain() ?: error("Parent not found")
        val newMsg = Message(
            conversationId = parent.conversationId,
            parentId = parent.parentId, // sibling branch
            role = Role.USER,
            content = newContent
        )
        // update parent's branchChildren
        val updatedParent = parent.copy(branchChildren = parent.branchChildren + newMsg.id)
        db.messageDao().upsert(updatedParent.toEntity())
        db.messageDao().upsert(newMsg.toEntity())
        return newMsg
    }

    suspend fun getProviderWithModel(conversation: Conversation): Pair<ProviderConfig, ModelInfo> {
        val provider = providerRepository.getProvider(conversation.providerId) ?: error("Provider not found")
        val model = providerRepository.getModel(conversation.modelId) ?: ModelInfo(id = conversation.modelId, name = conversation.modelId, providerId = provider.id)
        return provider to model
    }

    fun buildChatProvider(provider: ProviderConfig): ChatProvider = ProviderFactory.create(provider)

    // Mappers
    private fun ConversationEntity.toDomain() = Conversation(
        id = id, title = title, providerId = providerId, modelId = modelId,
        createdAt = createdAt, updatedAt = updatedAt, systemPrompt = systemPrompt,
        memoryEnabled = memoryEnabled, workspaceId = workspaceId, pinned = pinned, folder = folder
    )
    private fun Conversation.toEntity() = ConversationEntity(
        id = id, title = title, providerId = providerId, modelId = modelId,
        createdAt = createdAt, updatedAt = updatedAt, systemPrompt = systemPrompt,
        memoryEnabled = memoryEnabled, workspaceId = workspaceId, pinned = pinned, folder = folder
    )
    private fun MessageEntity.toDomain() = Message(
        id = id, conversationId = conversationId, parentId = parentId, role = role,
        content = content, reasoning = reasoning, createdAt = createdAt, tokensUsed = tokensUsed,
        attachments = attachments, toolCalls = toolCalls, branchChildren = branchChildren, metadata = metadata
    )
    private fun Message.toEntity() = MessageEntity(
        id = id, conversationId = conversationId, parentId = parentId, role = role,
        content = content, reasoning = reasoning, createdAt = createdAt, tokensUsed = tokensUsed,
        attachments = attachments, toolCalls = toolCalls, branchChildren = branchChildren, metadata = metadata
    )
}
