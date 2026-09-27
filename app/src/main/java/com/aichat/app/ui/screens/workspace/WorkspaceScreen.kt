package com.aichat.app.ui.screens.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    onBack: () -> Unit,
    viewModel: WorkspaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace Agent") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { viewModel.toggleAgentMode() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Agent Mode", tint = if (uiState.isAgentMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createFile() }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // File explorer
            Card(modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Files", style = MaterialTheme.typography.titleMedium)
                    LazyColumn {
                        items(uiState.files) { file ->
                            ListItem(
                                headlineContent = { Text(file.name) },
                                leadingContent = { Icon(Icons.Default.Code, null) },
                                supportingContent = { Text(file.path, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
            // Editor / Agent log
            Card(modifier = Modifier.weight(2f).fillMaxHeight().padding(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Agent Environment", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Workspace agent can:\n• Read/write files\n• Execute code (sandboxed)\n• Use MCP tools\n• Search web\n• Manage memory\n\nAgent mode: ${if (uiState.isAgentMode) "ON - AI can modify workspace" else "OFF"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.agentPrompt,
                        onValueChange = { viewModel.updateAgentPrompt(it) },
                        label = { Text("Agent task (e.g., 'Create a Python script to analyze data')") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.runAgent() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Run Agent")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Logs:", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(uiState.logs) { log ->
                            Text(log, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
