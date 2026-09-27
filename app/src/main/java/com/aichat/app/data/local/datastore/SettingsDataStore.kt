package com.aichat.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "aichat_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme") // system, light, dark
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_PROVIDER = stringPreferencesKey("default_provider")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val STREAMING_ENABLED = booleanPreferencesKey("streaming_enabled")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val SEARCH_PROVIDER = stringPreferencesKey("search_provider") // brave, tavily, serpapi, disabled
        val SEARCH_API_KEY = stringPreferencesKey("search_api_key")
        val MCP_ENABLED = booleanPreferencesKey("mcp_enabled")
        val AUTO_TITLE = booleanPreferencesKey("auto_title")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val SHOW_REASONING = booleanPreferencesKey("show_reasoning")
        val HAPTICS = booleanPreferencesKey("haptics")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "system" }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }
    val streamingEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.STREAMING_ENABLED] ?: true }
    val memoryEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MEMORY_ENABLED] ?: true }
    val mcpEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MCP_ENABLED] ?: false }
    val autoTitle: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_TITLE] ?: true }
    val fontScale: Flow<Float> = context.dataStore.data.map { it[Keys.FONT_SCALE] ?: 1f }
    val searchProvider: Flow<String> = context.dataStore.data.map { it[Keys.SEARCH_PROVIDER] ?: "brave" }

    suspend fun setTheme(value: String) = context.dataStore.edit { it[Keys.THEME] = value }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    suspend fun setDefaultProvider(id: String) = context.dataStore.edit { it[Keys.DEFAULT_PROVIDER] = id }
    suspend fun setDefaultModel(id: String) = context.dataStore.edit { it[Keys.DEFAULT_MODEL] = id }
    suspend fun setSearchProvider(provider: String, apiKey: String = "") = context.dataStore.edit {
        it[Keys.SEARCH_PROVIDER] = provider
        it[Keys.SEARCH_API_KEY] = apiKey
    }
    suspend fun setMcpEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.MCP_ENABLED] = enabled }
    suspend fun setFontScale(scale: Float) = context.dataStore.edit { it[Keys.FONT_SCALE] = scale }
}
