package com.aichat.app.ui.screens.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun ModelPickerScreen(
    onBack: () -> Unit,
    viewModel: ModelPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Models") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Provider selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.providers.forEach { provider ->
                    FilterChip(
                        selected = uiState.selectedProviderId == provider.id,
                        onClick = { viewModel.selectProvider(provider.id) },
                        label = { Text(provider.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.models) { model ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(model.name) },
                            supportingContent = {
                                Column {
                                    Text("${model.contextLength / 1000}k context • ${model.description}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                        if (model.supportsVision) Badge { Text("Vision") }
                                        if (model.supportsTools) Badge { Text("Tools") }
                                        if (model.supportsReasoning) Badge { Text("Reasoning") }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
