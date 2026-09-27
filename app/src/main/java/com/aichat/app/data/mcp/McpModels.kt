package com.aichat.app.data.mcp

import kotlinx.serialization.Serializable

@Serializable
data class McpServerConfig(
    val id: String,
    val name: String,
    val url: String,
    val transport: McpTransport = McpTransport.SSE,
    val enabled: Boolean = true,
    val headers: Map<String, String> = emptyMap()
)

enum class McpTransport { STDIO, SSE, WEBSOCKET }

@Serializable
data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: String,
    val serverId: String
)

@Serializable
data class McpResource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null
)

@Serializable
data class McpPrompt(
    val name: String,
    val description: String? = null,
    val arguments: List<McpPromptArg> = emptyList()
)

@Serializable
data class McpPromptArg(
    val name: String,
    val description: String? = null,
    val required: Boolean = false
)

data class McpCallResult(
    val content: List<McpContent>,
    val isError: Boolean = false
)

@Serializable
data class McpContent(
    val type: String,
    val text: String? = null
)
