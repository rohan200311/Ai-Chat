package com.aichat.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToMcp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = uiState.theme == "system", onClick = { viewModel.setTheme("system") }, label = { Text("System") })
                            FilterChip(selected = uiState.theme == "light", onClick = { viewModel.setTheme("light") }, label = { Text("Light") })
                            FilterChip(selected = uiState.theme == "dark", onClick = { viewModel.setTheme("dark") }, label = { Text("Dark") })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dynamic Color (Material You)")
                            Switch(checked = uiState.dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) })
                        }
                        Text("Font Scale: ${uiState.fontScale}")
                        Slider(value = uiState.fontScale, onValueChange = { viewModel.setFontScale(it) }, valueRange = 0.8f..1.4f)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Providers & Models", style = MaterialTheme.typography.titleMedium)
                        Text("Manage API keys, base URLs, custom headers", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onNavigateToProviders, modifier = Modifier.fillMaxWidth()) { Text("Manage Providers") }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Search & Tools", style = MaterialTheme.typography.titleMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MCP (Model Context Protocol)")
                            Switch(checked = uiState.mcpEnabled, onCheckedChange = { viewModel.setMcpEnabled(it) })
                        }
                        Button(onClick = onNavigateToMcp, modifier = Modifier.fillMaxWidth()) { Text("MCP Servers") }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Auto-generate titles")
                            Switch(checked = uiState.autoTitle, onCheckedChange = { viewModel.setAutoTitle(it) })
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("About", style = MaterialTheme.typography.titleMedium)
                        Text("AI Chat v1.0.0\nBuilt with Kotlin, Compose, Material 3\nSupports OpenAI, Anthropic, Gemini, Groq, Ollama, OpenRouter, custom endpoints\nMarkdown: code, tables, LaTeX, Mermaid\nExtras: branching, memory, variables, search, QR, workspace, MCP, web PWA", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
