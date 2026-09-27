package com.aichat.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val type: String
)

@Serializable
data class SharedChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = 0
)

object PromptVariableEngine {
    fun render(template: String, vars: Map<String, String>): String {
        var result = template
        Regex("\\{\\{\\s*(\\w+)(?::([^}]+))?\\s*\\}\\}").findAll(template).forEach { m ->
            val key = m.groupValues[1]
            val fallback = m.groupValues.getOrNull(2) ?: ""
            result = result.replace(m.value, vars[key] ?: fallback)
        }
        return result
    }
}
