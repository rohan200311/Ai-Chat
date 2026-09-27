package com.aichat.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable
    data object Home : Route()

    @Serializable
    data class Chat(val conversationId: String) : Route()

    @Serializable
    data object Settings : Route()

    @Serializable
    data object ProviderSettings : Route()

    @Serializable
    data object ModelPicker : Route()

    @Serializable
    data object Workspace : Route()

    @Serializable
    data class WorkspaceDetail(val workspaceId: String) : Route()

    @Serializable
    data object Search : Route()

    @Serializable
    data object McpSettings : Route()
}
