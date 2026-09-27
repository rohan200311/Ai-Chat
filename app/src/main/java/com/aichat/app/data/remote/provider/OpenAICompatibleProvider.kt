package com.aichat.app.data.remote.provider

import com.aichat.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

interface ChatProvider {
    suspend fun listModels(): List<ModelInfo>
    suspend fun chat(
        messages: List<Message>,
        model: ModelInfo,
        provider: ProviderConfig,
        systemPrompt: String? = null,
        tools: List<McpTool> = emptyList(),
        stream: Boolean = true
    ): Flow<ChatChunk>

    suspend fun generateTitle(messages: List<Message>, model: ModelInfo, provider: ProviderConfig): String
}

data class ChatChunk(
    val content: String,
    val reasoning: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val isDone: Boolean = false,
    val usage: TokenUsage? = null
)

data class TokenUsage(val prompt: Int, val completion: Int, val total: Int)

@Serializable
data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: String
)

@Serializable
data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val stream: Boolean = true,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val tools: List<OpenAITool>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null
)

@Serializable
data class OpenAIMessage(
    val role: String,
    val content: List<OpenAIContent>? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAIToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null
) {
    companion object {
        fun text(role: String, text: String) = OpenAIMessage(
            role = role,
            content = listOf(OpenAIContent(type = "text", text = text))
        )
    }
}

@Serializable
data class OpenAIContent(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null
)

@Serializable
data class ImageUrl(val url: String)

@Serializable
data class OpenAITool(
    val type: String = "function",
    val function: OpenAIFunction
)

@Serializable
data class OpenAIFunction(
    val name: String,
    val description: String,
    val parameters: kotlinx.serialization.json.JsonElement? = null
)

@Serializable
data class OpenAIToolCall(
    val id: String,
    val type: String = "function",
    val function: OpenAIFunctionCall
)

@Serializable
data class OpenAIFunctionCall(
    val name: String,
    val arguments: String
)

class OpenAICompatibleProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : ChatProvider {

    override suspend fun listModels(): List<ModelInfo> = emptyList() // overridden by repo with caching

    override suspend fun chat(
        messages: List<Message>,
        model: ModelInfo,
        provider: ProviderConfig,
        systemPrompt: String?,
        tools: List<McpTool>,
        stream: Boolean
    ): Flow<ChatChunk> = flow {
        val openAiMessages = buildList {
            if (!systemPrompt.isNullOrBlank()) add(OpenAIMessage.text("system", systemPrompt))
            messages.forEach { msg ->
                val role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                    Role.TOOL -> "tool"
                }
                val contents = mutableListOf<OpenAIContent>()
                if (msg.content.isNotBlank()) contents.add(OpenAIContent(type = "text", text = msg.content))
                msg.attachments.filter { it.type == AttachmentType.IMAGE && it.base64Data != null }
                    .forEach { att ->
                        contents.add(OpenAIContent(type = "image_url", imageUrl = ImageUrl("data:${att.mimeType};base64,${att.base64Data}")))
                    }
                if (msg.attachments.any { it.extractedText != null }) {
                    val extracted = msg.attachments.mapNotNull { it.extractedText }.joinToString("\n\n")
                    if (extracted.isNotBlank()) contents.add(OpenAIContent(type = "text", text = "\n\n[Attachments content]:\n$extracted"))
                }
                add(OpenAIMessage(role = role, content = contents.ifEmpty { null }))
            }
        }

        val requestBody = OpenAIChatRequest(
            model = model.id,
            messages = openAiMessages,
            stream = stream,
            tools = tools.map {
                OpenAITool(function = OpenAIFunction(name = it.name, description = it.description, parameters = try { json.parseToJsonElement(it.inputSchema) } catch (_: Exception) { null }))
            }.ifEmpty { null }
        )

        val httpRequest = Request.Builder()
            .url("${provider.baseUrl.trimEnd('/')}/chat/completions")
            .header("Authorization", "Bearer ${provider.apiKey}")
            .header("Content-Type", "application/json")
            .apply { provider.customHeaders.forEach { (k, v) -> header(k, v) } }
            .post(json.encodeToString(OpenAIChatRequest.serializer(), requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        if (!stream) {
            client.newCall(httpRequest).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                // Parse non-streaming
                // Simplified: extract content
                val text = extractContentFromNonStreaming(body)
                emit(ChatChunk(content = text, isDone = true))
            }
            return@flow
        }

        client.newCall(httpRequest).execute().use { response ->
            val source = response.body?.source() ?: return@flow
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") {
                    emit(ChatChunk(content = "", isDone = true))
                    break
                }
                try {
                    val chunk = json.decodeFromString<OpenAIStreamChunk>(data)
                    val delta = chunk.choices.firstOrNull()?.delta
                    val content = delta?.content ?: ""
                    val reasoning = delta?.reasoningContent
                    val toolCalls = delta?.toolCalls?.map {
                        ToolCall(id = it.id ?: "", name = it.function?.name ?: "", arguments = it.function?.arguments ?: "")
                    } ?: emptyList()
                    if (content.isNotEmpty() || reasoning != null || toolCalls.isNotEmpty()) {
                        emit(ChatChunk(content = content, reasoning = reasoning, toolCalls = toolCalls))
                    }
                } catch (_: Exception) {
                    // ignore parse errors
                }
            }
        }
    }

    private fun extractContentFromNonStreaming(body: String): String {
        return try {
            val obj = Json.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject
            val choices = obj?.get("choices") as? kotlinx.serialization.json.JsonArray
            val first = choices?.firstOrNull() as? kotlinx.serialization.json.JsonObject
            val message = first?.get("message") as? kotlinx.serialization.json.JsonObject
            val content = message?.get("content")
            content?.toString()?.trim('"') ?: body
        } catch (_: Exception) { body }
    }

    override suspend fun generateTitle(messages: List<Message>, model: ModelInfo, provider: ProviderConfig): String {
        return "New Chat"
    }

    @Serializable
    private data class OpenAIStreamChunk(
        val choices: List<StreamChoice>
    )
    @Serializable
    private data class StreamChoice(
        val delta: StreamDelta
    )
    @Serializable
    private data class StreamDelta(
        val content: String? = null,
        @SerialName("reasoning_content") val reasoningContent: String? = null,
        @SerialName("tool_calls") val toolCalls: List<StreamToolCall>? = null
    )
    @Serializable
    private data class StreamToolCall(
        val id: String? = null,
        val function: StreamFunction? = null
    )
    @Serializable
    private data class StreamFunction(
        val name: String? = null,
        val arguments: String? = null
    )
}
