package com.aichat.app.data.mcp

import com.aichat.app.data.remote.provider.McpTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpClient @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _servers = MutableStateFlow<List<McpServerConfig>>(emptyList())
    val servers: StateFlow<List<McpServerConfig>> = _servers

    private val _tools = MutableStateFlow<List<McpToolDefinition>>(emptyList())
    val tools: StateFlow<List<McpToolDefinition>> = _tools

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun addServer(config: McpServerConfig) {
        _servers.value = _servers.value + config
        // In real implementation: connect via SSE/WebSocket and perform MCP handshake
        // List tools via MCP protocol: initialize -> tools/list
    }

    fun removeServer(id: String) {
        _servers.value = _servers.value.filterNot { it.id == id }
        _tools.value = _tools.value.filterNot { it.serverId == id }
    }

    suspend fun discoverTools(server: McpServerConfig): List<McpToolDefinition> {
        return try {
            val request = Request.Builder()
                .url("${server.url.trimEnd('/')}/mcp/tools/list")
                .apply { server.headers.forEach { (k, v) -> header(k, v) } }
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                json.decodeFromString<List<McpToolDefinition>>(body)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun callTool(serverId: String, toolName: String, arguments: String): McpCallResult {
        val server = _servers.value.find { it.id == serverId } ?: return McpCallResult(listOf(McpContent("text", "Server not found")), true)
        return try {
            val request = Request.Builder()
                .url("${server.url.trimEnd('/')}/mcp/tools/call")
                .header("Content-Type", "application/json")
                .apply { server.headers.forEach { (k, v) -> header(k, v) } }
                .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), """{"name":"$toolName","arguments":$arguments}"""))
                .build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                McpCallResult(listOf(McpContent("text", body)), !resp.isSuccessful)
            }
        } catch (e: Exception) {
            McpCallResult(listOf(McpContent("text", e.message ?: "Error")), true)
        }
    }

    fun asProviderTools(): List<McpTool> {
        return _tools.value.map { McpTool(name = it.name, description = it.description, inputSchema = it.inputSchema) }
    }

    // Android-native Mobile-MCP via Intent
    fun discoverAndroidMcpServices(context: android.content.Context): List<McpToolDefinition> {
        // Query PackageManager for services with intent filter com.aichat.MCP_TOOL
        return emptyList() // placeholder for real intent discovery
    }
}
