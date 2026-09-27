package com.aichat.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.aichat.app.ui.screens.chat.ChatScreen
import com.aichat.app.ui.screens.home.HomeScreen
import com.aichat.app.ui.screens.models.ModelPickerScreen
import com.aichat.app.ui.screens.settings.ProviderSettingsScreen
import com.aichat.app.ui.screens.settings.SettingsScreen
import com.aichat.app.ui.screens.workspace.WorkspaceScreen

@Composable
fun AiChatNavGraph(
    navController: NavHostController,
    startDestination: Route = Route.Home
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Home> {
            HomeScreen(
                onNavigateToChat = { id -> navController.navigate(Route.Chat(id)) },
                onNavigateToSettings = { navController.navigate(Route.Settings) },
                onNavigateToModelPicker = { navController.navigate(Route.ModelPicker) },
                onNavigateToWorkspace = { navController.navigate(Route.Workspace) }
            )
        }
        composable<Route.Chat> { backStack ->
            val args = backStack.toRoute<Route.Chat>()
            ChatScreen(
                conversationId = args.conversationId,
                onBack = { navController.popBackStack() },
                onNavigateToModelPicker = { navController.navigate(Route.ModelPicker) },
                onNavigateToWorkspace = { navController.navigate(Route.Workspace) }
            )
        }
        composable<Route.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToProviders = { navController.navigate(Route.ProviderSettings) },
                onNavigateToMcp = { navController.navigate(Route.McpSettings) }
            )
        }
        composable<Route.ProviderSettings> {
            ProviderSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.ModelPicker> {
            ModelPickerScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Workspace> {
            WorkspaceScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.McpSettings> {
            // Placeholder, reuses Settings for now
            SettingsScreen(onBack = { navController.popBackStack() }, onNavigateToProviders = {}, onNavigateToMcp = {})
        }
    }
}
