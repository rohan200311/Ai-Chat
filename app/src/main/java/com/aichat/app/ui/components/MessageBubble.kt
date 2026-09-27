package com.aichat.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aichat.app.domain.model.Message
import com.aichat.app.domain.model.Role
import com.aichat.app.ui.components.markdown.MarkdownRenderer

@Composable
fun MessageBubble(
    message: Message,
    onCopy: () -> Unit,
    onBranch: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    showBranching: Boolean = true
) {
    val isUser = message.role == Role.USER
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    var showActions by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(onClick = { showActions = !showActions }, onLongClick = { showActions = true }),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = bubbleColor,
            tonalElevation = if (isUser) 2.dp else 1.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (message.reasoning != null) {
                    ReasoningBlock(reasoning = message.reasoning)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isUser) {
                    Text(text = message.content, color = textColor, style = MaterialTheme.typography.bodyLarge)
                } else {
                    MarkdownRenderer(content = message.content, modifier = Modifier.fillMaxWidth())
                }

                if (message.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AttachmentPreview(attachments = message.attachments)
                }

                if (message.toolCalls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ToolCallsView(toolCalls = message.toolCalls)
                }
            }
        }

        AnimatedVisibility(visible = showActions) {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
                if (showBranching) {
                    FilledTonalIconButton(onClick = onBranch, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ForkRight, contentDescription = "Branch", modifier = Modifier.size(16.dp))
                    }
                }
                if (!isUser) {
                    FilledTonalIconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (message.branchChildren.isNotEmpty()) {
            Text(
                text = "${message.branchChildren.size} branches",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ReasoningBlock(reasoning: String) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide reasoning" else "Show reasoning", style = MaterialTheme.typography.labelMedium)
            }
            if (expanded) {
                Text(text = reasoning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AttachmentPreview(attachments: List<com.aichat.app.domain.model.Attachment>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        attachments.forEach { att ->
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = "📎 ${att.name} (${att.type})",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ToolCallsView(toolCalls: List<com.aichat.app.domain.model.ToolCall>) {
    Column {
        toolCalls.forEach { call ->
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("🔧 ${call.name}", style = MaterialTheme.typography.labelMedium)
                    Text(call.arguments.take(200), style = MaterialTheme.typography.bodySmall)
                    call.result?.let { Text("→ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                }
            }
        }
    }
}
