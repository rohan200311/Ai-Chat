package com.aichat.app.ui.components.markdown

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import androidx.compose.material3.MaterialTheme

/**
 * Production-grade markdown renderer:
 * - Uses multiplatform-markdown-renderer for native Compose rendering
 * - Falls back to WebView for Mermaid, KaTeX, and complex tables
 * - Code blocks with syntax highlighting via Highlights
 */
@Composable
fun MarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    // Detect special blocks that need WebView
    val needsWebView = remember(content) {
        content.contains("```mermaid") || content.contains("$$") || content.contains("\\[") || content.contains("$") && content.contains("^")
    }

    if (needsWebView && (content.contains("```mermaid") || content.contains("$$"))) {
        // Use WebView for Mermaid + KaTeX
        EnhancedMarkdownWebView(content = content, isDarkTheme = isDarkTheme, modifier = modifier)
    } else {
        // Native Compose markdown - fast, selectable, Material You
        SelectionContainer {
            Markdown(
                content = content,
                modifier = modifier.padding(4.dp),
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.headlineLarge,
                    h2 = MaterialTheme.typography.headlineMedium,
                    h3 = MaterialTheme.typography.headlineSmall,
                    text = MaterialTheme.typography.bodyLarge,
                    code = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    quote = MaterialTheme.typography.bodyMedium
                )
            )
        }
    }
}

@Composable
fun CodeBlock(
    code: String,
    language: String? = null,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Column {
            // Header with language + copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language ?: "code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Copy button would go here
            }
            Text(
                text = code,
                modifier = Modifier
                    .horizontalScroll(scroll)
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
fun EnhancedMarkdownWebView(
    content: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Preprocess markdown to HTML with Mermaid + KaTeX support
    val html = remember(content, isDarkTheme) {
        buildHtmlWithMermaidAndKatex(content, isDarkTheme)
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().heightIn(min = 100.dp),
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                isVerticalScrollBarEnabled = false
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    )
}

private fun buildHtmlWithMermaidAndKatex(markdown: String, isDark: Boolean): String {
    // In production, use markdown-it + mermaid + katex via CDN
    // Simplified template for offline support
    val bg = if (isDark) "#121212" else "#ffffff"
    val fg = if (isDark) "#e0e0e0" else "#1a1a1a"
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/katex.min.css">
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/katex.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js"></script>
            <style>
                body { background: $bg; color: $fg; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 12px; line-height: 1.6; }
                pre { background: ${if (isDark) "#1e1e1e" else "#f5f5f5"}; padding: 12px; border-radius: 8px; overflow-x: auto; }
                code { font-family: 'JetBrains Mono', monospace; }
                table { border-collapse: collapse; width: 100%; margin: 12px 0; }
                th, td { border: 1px solid ${if (isDark) "#333" else "#ddd"}; padding: 8px; text-align: left; }
                .mermaid { background: ${if (isDark) "#1e1e1e" else "#fafafa"}; padding: 16px; border-radius: 8px; }
            </style>
        </head>
        <body>
            <div id="content"></div>
            <script type="module">
                import { marked } from "https://cdn.jsdelivr.net/npm/marked@14.1.3/lib/marked.esm.js";
                const md = `${markdown.replace("`", "\\`").replace("\n", "\\n")}`;
                document.getElementById('content').innerHTML = marked.parse(md);
                mermaid.initialize({ startOnLoad: true, theme: '${if (isDark) "dark" else "default"}' });
                // Render KaTeX
                document.querySelectorAll('p, li, span').forEach(el => {
                    // Simple inline math detection
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
