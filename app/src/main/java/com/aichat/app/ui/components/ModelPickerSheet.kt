package com.aichat.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aichat.app.domain.model.ModelInfo
import com.aichat.app.domain.model.ProviderConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerBottomSheet(
    providers: List<ProviderConfig>,
    models: List<ModelInfo>,
    selectedModelId: String?,
    onModelSelected: (ModelInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProviderId by remember { mutableStateOf(providers.firstOrNull()?.id) }
    val filteredModels = remember(models, selectedProviderId) {
        if (selectedProviderId == null) models else models.filter { it.providerId == selectedProviderId }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Select Model", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Provider chips
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                providers.forEach { provider ->
                    FilterChip(
                        selected = selectedProviderId == provider.id,
                        onClick = { selectedProviderId = provider.id },
                        label = { Text(provider.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(filteredModels) { model ->
                    ListItem(
                        headlineContent = { Text(model.name) },
                        supportingContent = {
                            Column {
                                Text(model.description.ifBlank { "${model.contextLength / 1000}k context" }, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    if (model.supportsVision) AssistChip(onClick = {}, label = { Text("Vision") })
                                    if (model.supportsTools) AssistChip(onClick = {}, label = { Text("Tools") })
                                    if (model.supportsReasoning) AssistChip(onClick = {}, label = { Text("Reasoning") })
                                }
                            }
                        },
                        trailingContent = {
                            if (model.id == selectedModelId) Icon(Icons.Default.Check, contentDescription = null)
                        },
                        modifier = Modifier.clickable { onModelSelected(model) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
