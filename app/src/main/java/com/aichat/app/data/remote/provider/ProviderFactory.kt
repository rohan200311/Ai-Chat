package com.aichat.app.data.remote.provider

import com.aichat.app.domain.model.ProviderConfig
import com.aichat.app.domain.model.ProviderType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ProviderFactory {
    fun create(provider: ProviderConfig): ChatProvider {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    if (provider.apiKey.isNotBlank()) {
                        when (provider.type) {
                            ProviderType.ANTHROPIC -> header("x-api-key", provider.apiKey)
                            else -> header("Authorization", "Bearer ${provider.apiKey}")
                        }
                    }
                    provider.customHeaders.forEach { (k, v) -> header(k, v) }
                }.build()
                chain.proceed(req)
            }
            .build()

        return when (provider.type) {
            ProviderType.ANTHROPIC -> AnthropicProvider(client)
            ProviderType.GOOGLE -> GeminiProvider(client)
            else -> OpenAICompatibleProvider(client)
        }
    }
}

class AnthropicProvider(private val client: OkHttpClient) : ChatProvider {
    override suspend fun listModels(): List<com.aichat.app.domain.model.ModelInfo> = listOf(
        com.aichat.app.domain.model.ModelInfo(id = "claude-3-5-sonnet-20241022", name = "Claude 3.5 Sonnet", providerId = "anthropic", contextLength = 200000, supportsVision = true, supportsTools = true),
        com.aichat.app.domain.model.ModelInfo(id = "claude-3-5-haiku-20241022", name = "Claude 3.5 Haiku", providerId = "anthropic", contextLength = 200000)
    )
    override suspend fun chat(messages: List<com.aichat.app.domain.model.Message>, model: com.aichat.app.domain.model.ModelInfo, provider: ProviderConfig, systemPrompt: String?, tools: List<McpTool>, stream: Boolean) = OpenAICompatibleProvider(client).chat(messages, model, provider, systemPrompt, tools, stream)
    override suspend fun generateTitle(messages: List<com.aichat.app.domain.model.Message>, model: com.aichat.app.domain.model.ModelInfo, provider: ProviderConfig): String = "New Chat"
}

class GeminiProvider(private val client: OkHttpClient) : ChatProvider {
    override suspend fun listModels(): List<com.aichat.app.domain.model.ModelInfo> = listOf(
        com.aichat.app.domain.model.ModelInfo(id = "gemini-2.0-flash", name = "Gemini 2.0 Flash", providerId = "google", contextLength = 1000000, supportsVision = true, supportsTools = true),
        com.aichat.app.domain.model.ModelInfo(id = "gemini-1.5-pro", name = "Gemini 1.5 Pro", providerId = "google", contextLength = 2000000, supportsVision = true)
    )
    override suspend fun chat(messages: List<com.aichat.app.domain.model.Message>, model: com.aichat.app.domain.model.ModelInfo, provider: ProviderConfig, systemPrompt: String?, tools: List<McpTool>, stream: Boolean) = OpenAICompatibleProvider(client).chat(messages, model, provider, systemPrompt, tools, stream)
    override suspend fun generateTitle(messages: List<com.aichat.app.domain.model.Message>, model: com.aichat.app.domain.model.ModelInfo, provider: ProviderConfig): String = "New Chat"
}
