package com.aichat.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.domain.model.ProviderConfig
import com.aichat.app.domain.model.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProviderSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Providers") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showQrDialog = true }) { Icon(Icons.Default.QrCode, contentDescription = "QR") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.providers, key = { it.id }) { provider ->
                ProviderCard(provider = provider, onDelete = { viewModel.deleteProvider(provider.id) }, onExport = { viewModel.exportProvider(provider) })
            }
        }

        if (showAddDialog) {
            AddProviderDialog(
                onDismiss = { showAddDialog = false },
                onSave = { config ->
                    viewModel.addProvider(config)
                    showAddDialog = false
                }
            )
        }

        if (showQrDialog) {
            QrImportExportDialog(
                exportJson = uiState.exportJson,
                onImport = { json -> viewModel.importProviders(json) },
                onDismiss = { showQrDialog = false }
            )
        }
    }
}

@Composable
fun ProviderCard(provider: ProviderConfig, onDelete: () -> Unit, onExport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null) }
            }
            Text("${provider.type} • ${provider.baseUrl}", style = MaterialTheme.typography.bodySmall)
            Text("API Key: ${if (provider.apiKey.isBlank()) "Not set" else "••••${provider.apiKey.takeLast(4)}"}", style = MaterialTheme.typography.labelSmall)
            if (provider.customHeaders.isNotEmpty()) {
                Text("Custom Headers: ${provider.customHeaders.size}", style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onExport, label = { Text("Export QR") })
                if (!provider.isEnabled) Badge { Text("Disabled") }
            }
        }
    }
}

@Composable
fun AddProviderDialog(onDismiss: () -> Unit, onSave: (ProviderConfig) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ProviderType.OPENAI) }
    var baseUrl by remember { mutableStateOf("https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf("") }
    var customHeadersText by remember { mutableStateOf("") } // JSON

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                // Type dropdown simplified
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = customHeadersText, onValueChange = { customHeadersText = it }, label = { Text("Custom Headers JSON (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val headers = try {
                    kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(customHeadersText.ifBlank { "{}" })
                } catch (_: Exception) { emptyMap() }
                onSave(ProviderConfig(name = name, type = type, baseUrl = baseUrl, apiKey = apiKey, customHeaders = headers))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun QrImportExportDialog(exportJson: String, onImport: (String) -> Unit, onDismiss: () -> Unit) {
    var importText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR Import / Export") },
        text = {
            Column {
                Text("Export (JSON):", style = MaterialTheme.typography.labelMedium)
                Text(exportJson.take(500), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = importText, onValueChange = { importText = it }, label = { Text("Paste JSON or QR content") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onImport(importText) }) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
