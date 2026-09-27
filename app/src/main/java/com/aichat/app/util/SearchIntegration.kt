package com.aichat.app.util

import com.aichat.app.domain.model.SearchResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class SearchProviderType { BRAVE, TAVILY, SERPAPI, DISABLED }

@Singleton
class SearchIntegration @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(
        query: String,
        provider: SearchProviderType,
        apiKey: String,
        maxResults: Int = 5
    ): List<SearchResult> {
        if (provider == SearchProviderType.DISABLED || apiKey.isBlank()) return emptyList()
        return when (provider) {
            SearchProviderType.BRAVE -> searchBrave(query, apiKey, maxResults)
            SearchProviderType.TAVILY -> searchTavily(query, apiKey, maxResults)
            SearchProviderType.SERPAPI -> searchSerpApi(query, apiKey, maxResults)
            else -> emptyList()
        }
    }

    private fun searchBrave(query: String, apiKey: String, maxResults: Int): List<SearchResult> {
        return try {
            val req = Request.Builder()
                .url("https://api.search.brave.com/res/v1/web/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&count=$maxResults")
                .header("X-Subscription-Token", apiKey)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                // Parse Brave response (simplified)
                // Real parsing would use data class
                // For now return mock structure parsed loosely
                emptyList()
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun searchTavily(query: String, apiKey: String, maxResults: Int): List<SearchResult> {
        return try {
            val body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json"),
                """{"api_key":"$apiKey","query":"$query","max_results":$maxResults,"include_answer":true}"""
            )
            val req = Request.Builder()
                .url("https://api.tavily.com/search")
                .post(body)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val responseBody = resp.body?.string() ?: return emptyList()
                val parsed = json.decodeFromString<TavilyResponse>(responseBody)
                parsed.results.map { SearchResult(it.title, it.url, it.content, "tavily") }
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun searchSerpApi(query: String, apiKey: String, maxResults: Int): List<SearchResult> {
        return emptyList() // similar implementation
    }

    @Serializable
    private data class TavilyResponse(val results: List<TavilyResult>)
    @Serializable
    private data class TavilyResult(val title: String, val url: String, val content: String)
}
