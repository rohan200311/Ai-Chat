package com.aichat.app.data.repository

import com.aichat.app.data.local.db.AppDatabase
import com.aichat.app.data.local.db.ModelEntity
import com.aichat.app.data.local.db.ProviderEntity
import com.aichat.app.domain.model.ModelInfo
import com.aichat.app.domain.model.ProviderConfig
import com.aichat.app.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val db: AppDatabase
) {
    fun observeProviders(): Flow<List<ProviderConfig>> =
        db.providerDao().observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<ProviderConfig> = db.providerDao().getAll().map { it.toDomain() }

    suspend fun getProvider(id: String): ProviderConfig? = db.providerDao().getAll().find { it.id == id }?.toDomain()

    suspend fun getModel(id: String): ModelInfo? = db.modelDao().getAll().find { it.id == id }?.toDomain()

    fun observeModels(providerId: String): Flow<List<ModelInfo>> =
        db.modelDao().observeByProvider(providerId).map { list -> list.map { it.toDomain() } }

    suspend fun upsertProvider(config: ProviderConfig) {
        db.providerDao().upsert(config.toEntity())
        // Seed default models if empty
        if (config.enabledModels.isEmpty()) {
            val defaults = defaultModelsFor(config)
            db.modelDao().upsertAll(defaults.map { it.toEntity() })
        } else {
            db.modelDao().upsertAll(config.enabledModels.map {
                ModelInfo(id = it, name = it, providerId = config.id).toEntity()
            })
        }
    }

    suspend fun deleteProvider(id: String) {
        db.modelDao().deleteByProvider(id)
        db.providerDao().delete(id)
    }

    suspend fun exportProviders(): String {
        val providers = getAll()
        return kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(ProviderConfig.serializer()),
            providers
        )
    }

    suspend fun importProviders(json: String): List<ProviderConfig> {
        return try {
            val list = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<List<ProviderConfig>>(json)
            list.forEach { upsertProvider(it) }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun defaultModelsFor(config: ProviderConfig): List<ModelInfo> = when (config.type) {
        ProviderType.OPENAI -> listOf(
            ModelInfo("gpt-4o", "GPT-4o", config.id, 128000, true, true, false, 2.5, 10.0, "Flagship multimodal"),
            ModelInfo("gpt-4o-mini", "GPT-4o mini", config.id, 128000, true, true),
            ModelInfo("o1", "o1", config.id, 200000, false, true, true, 15.0, 60.0, "Reasoning model"),
            ModelInfo("o3-mini", "o3-mini", config.id, 200000, false, true, true)
        )
        ProviderType.ANTHROPIC -> listOf(
            ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", config.id, 200000, true, true, false, 3.0, 15.0),
            ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", config.id, 200000, true, true)
        )
        ProviderType.GOOGLE -> listOf(
            ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", config.id, 1000000, true, true),
            ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", config.id, 2000000, true, true)
        )
        ProviderType.GROQ -> listOf(
            ModelInfo("llama-3.3-70b-versatile", "Llama 3.3 70B", config.id, 128000, false, true),
            ModelInfo("mixtral-8x7b-32768", "Mixtral 8x7B", config.id, 32768)
        )
        ProviderType.MISTRAL -> listOf(
            ModelInfo("mistral-large-latest", "Mistral Large", config.id, 128000, false, true),
            ModelInfo("codestral-latest", "Codestral", config.id, 32000)
        )
        ProviderType.OLLAMA -> listOf(
            ModelInfo("llama3.2", "Llama 3.2", config.id, 8192),
            ModelInfo("qwen2.5-coder", "Qwen2.5 Coder", config.id, 32000)
        )
        ProviderType.OPENROUTER -> listOf(
            ModelInfo("openai/gpt-4o", "GPT-4o (via OpenRouter)", config.id, 128000, true, true),
            ModelInfo("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet (via OpenRouter)", config.id, 200000, true, true)
        )
        ProviderType.CUSTOM -> config.enabledModels.map { ModelInfo(it, it, config.id) }
    }

    private fun ProviderEntity.toDomain() = ProviderConfig(
        id = id, name = name, type = type, baseUrl = baseUrl, apiKey = apiKey,
        customHeaders = customHeaders, enabledModels = enabledModels, isEnabled = isEnabled,
        createdAt = createdAt, supportsVision = supportsVision, supportsTools = supportsTools, supportsStreaming = supportsStreaming
    )
    private fun ProviderConfig.toEntity() = ProviderEntity(
        id = id, name = name, type = type, baseUrl = baseUrl, apiKey = apiKey,
        customHeaders = customHeaders, enabledModels = enabledModels, isEnabled = isEnabled,
        createdAt = createdAt, supportsVision = supportsVision, supportsTools = supportsTools, supportsStreaming = supportsStreaming
    )
    private fun ModelEntity.toDomain() = ModelInfo(
        id = id, name = name, providerId = providerId, contextLength = contextLength,
        supportsVision = supportsVision, supportsTools = supportsTools, supportsReasoning = supportsReasoning,
        inputPricePer1M = inputPricePer1M, outputPricePer1M = outputPricePer1M, description = description
    )
    private fun ModelInfo.toEntity() = ModelEntity(
        id = id, name = name, providerId = providerId, contextLength = contextLength,
        supportsVision = supportsVision, supportsTools = supportsTools, supportsReasoning = supportsReasoning,
        inputPricePer1M = inputPricePer1M, outputPricePer1M = outputPricePer1M, description = description
    )
}
