package com.aichat.app.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.domain.model.Role
import com.aichat.app.ui.components.MessageBubble
import com.aichat.app.ui.components.ModelPickerBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onNavigateToModelPicker: () -> Unit,
    onNavigateToWorkspace: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it) }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it) }
    }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    LaunchedEffect(uiState.messages.size, uiState.streamingContent) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.conversation?.title ?: "Chat", maxLines = 1, style = MaterialTheme.typography.titleMedium)
                        Text(
                            uiState.selectedModel?.name ?: "Select model",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleModelPicker(true) }) {
                        Icon(Icons.Default.SmartToy, contentDescription = "Model")
                    }
                    IconButton(onClick = onNavigateToWorkspace) {
                        Icon(Icons.Default.Folder, contentDescription = "Workspace")
                    }
                    IconButton(onClick = { viewModel.toggleSearch(!uiState.searchEnabled) }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (uiState.searchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                onAttachImage = { imagePickerLauncher.launch("image/*") },
                onAttachFile = { filePickerLauncher.launch("*/*") },
                attachments = uiState.attachments,
                onRemoveAttachment = { viewModel.removeAttachment(it) },
                isLoading = uiState.isLoading
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onCopy = { /* copy to clipboard */ },
                        onBranch = { viewModel.branchFromMessage(message.id) },
                        onRegenerate = { viewModel.regenerateLast() },
                        onEdit = { /* edit */ }
                    )
                }

                if (uiState.isLoading) {
                    item {
                        StreamingBubble(content = uiState.streamingContent, reasoning = uiState.streamingReasoning)
                    }
                }
            }

            // Branch dialog
            if (uiState.showBranchDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.toggleModelPicker(false) },
                    title = { Text("Create Branch") },
                    text = {
                        var branchText by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = branchText,
                            onValueChange = { branchText = it },
                            label = { Text("New message for branch") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { /* handled inside */ }) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { /* dismiss */ }) { Text("Cancel") }
                    }
                )
            }
        }

        if (uiState.showModelPicker) {
            ModelPickerBottomSheet(
                providers = uiState.providers,
                models = uiState.models,
                selectedModelId = uiState.selectedModel?.id,
                onModelSelected = { viewModel.selectModel(it) },
                onDismiss = { viewModel.toggleModelPicker(false) }
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachFile: () -> Unit,
    attachments: List<com.aichat.app.domain.model.Attachment>,
    onRemoveAttachment: (String) -> Unit,
    isLoading: Boolean
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (attachments.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    attachments.forEach { att ->
                        InputChip(
                            selected = false,
                            onClick = { onRemoveAttachment(att.id) },
                            label = { Text(att.name.take(20)) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onAttachImage) { Icon(Icons.Default.Image, contentDescription = "Attach image") }
                IconButton(onClick = onAttachFile) { Icon(Icons.Default.AttachFile, contentDescription = "Attach file") }

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    placeholder = { Text("Message AI... (use {{variable}} for prompts)") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 6
                )

                FilledIconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank() || attachments.isNotEmpty(),
                    modifier = Modifier.clip(CircleShape)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun StreamingBubble(content: String, reasoning: String) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            if (reasoning.isNotBlank()) {
                Text("Thinking...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(reasoning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
