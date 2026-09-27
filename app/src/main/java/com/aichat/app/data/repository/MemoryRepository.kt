package com.aichat.app.data.repository

import com.aichat.app.data.local.db.AppDatabase
import com.aichat.app.data.local.db.MemoryEntity
import com.aichat.app.domain.model.MemoryEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val db: AppDatabase
) {
    suspend fun getRelevant(conversationId: String, query: String? = null): List<MemoryEntry> {
        val all = db.memoryDao().getRelevant(conversationId).map { it.toDomain() }
        // Simple relevance: filter by query if provided, else sort by importance
        return if (query.isNullOrBlank()) all
        else all.filter { it.value.contains(query, true) || it.key.contains(query, true) }
    }

    suspend fun save(entry: MemoryEntry) {
        db.memoryDao().upsert(entry.toEntity())
    }

    suspend fun delete(id: String) = db.memoryDao().delete(id)

    suspend fun summarizeConversation(messages: List<com.aichat.app.domain.model.Message>): MemoryEntry? {
        // Placeholder: in production, call LLM to summarize
        if (messages.size < 5) return null
        val summary = messages.takeLast(10).joinToString("\n") { "${it.role}: ${it.content.take(100)}" }
        return MemoryEntry(
            conversationId = messages.firstOrNull()?.conversationId,
            key = "summary_${System.currentTimeMillis()}",
            value = summary,
            importance = 0.8f
        )
    }

    private fun MemoryEntity.toDomain() = MemoryEntry(id, conversationId, key, value, importance, createdAt, lastAccessed)
    private fun MemoryEntry.toEntity() = MemoryEntity(id, conversationId, key, value, importance, createdAt, lastAccessed)
}
