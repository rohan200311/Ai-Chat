package com.aichat.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToModelPicker: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Chat") },
                actions = {
                    IconButton(onClick = onNavigateToWorkspace) { Icon(Icons.Default.Folder, contentDescription = "Workspace") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.createNewChat(onNavigateToChat) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Chat") }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.conversations.isEmpty()) {
                item {
                    EmptyState(onNewChat = { viewModel.createNewChat(onNavigateToChat) })
                }
            } else {
                items(uiState.conversations, key = { it.id }) { conv ->
                    ConversationCard(
                        title = conv.title,
                        model = conv.modelId,
                        updatedAt = conv.updatedAt,
                        onClick = { onNavigateToChat(conv.id) },
                        onDelete = { viewModel.deleteConversation(conv.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationCard(
    title: String,
    model: String,
    updatedAt: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        ListItem(
            headlineContent = { Text(title, maxLines = 1, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(model, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingContent = { Icon(Icons.Default.Chat, contentDescription = null) },
            trailingContent = {
                Text(
                    text = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(updatedAt)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
    }
}

@Composable
fun EmptyState(onNewChat: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Welcome to AI Chat", style = MaterialTheme.typography.headlineMedium)
        Text(
            "A polished ChatGPT-style client with:\n• Multi-provider support (OpenAI, Claude, Gemini, Groq, Ollama, custom)\n• Vision, PDF & DOCX parsing\n• Markdown with Mermaid & LaTeX\n• Branching, memory, prompt variables\n• MCP & workspace agent\n• QR import/export & search",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onNewChat) { Text("Start Chatting") }
    }
}
