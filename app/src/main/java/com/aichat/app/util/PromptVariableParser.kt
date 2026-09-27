package com.aichat.app.util

import com.aichat.app.domain.model.PromptVariable

object PromptVariableParser {
    private val variableRegex = Regex("""\{\{\s*([a-zA-Z0-9_]+)(?::([^}]+))?\s*\}\}""")

    fun extractVariables(template: String): List<PromptVariable> {
        return variableRegex.findAll(template).map { match ->
            val name = match.groupValues[1]
            val defaultVal = match.groupValues.getOrNull(2) ?: ""
            PromptVariable(name = name, defaultValue = defaultVal, required = defaultVal.isEmpty())
        }.distinctBy { it.name }.toList()
    }

    fun render(template: String, values: Map<String, String>): String {
        var result = template
        variableRegex.findAll(template).forEach { match ->
            val name = match.groupValues[1]
            val fallback = match.groupValues.getOrNull(2) ?: ""
            val value = values[name] ?: fallback
            result = result.replace(match.value, value)
        }
        return result
    }

    fun validate(template: String, values: Map<String, String>): List<String> {
        val vars = extractVariables(template)
        return vars.filter { it.required && values[it.name].isNullOrBlank() }.map { it.name }
    }
}
