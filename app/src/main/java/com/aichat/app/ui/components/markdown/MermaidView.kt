package com.aichat.app.ui.components.markdown

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MermaidView(
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val html = remember(code) {
        """
        <!DOCTYPE html>
        <html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <script src="https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js"></script>
        <style>body{margin:0;padding:16px;background:transparent} .mermaid{background:white;border-radius:12px;padding:12px}</style>
        </head><body>
        <div class="mermaid">${code}</div>
        <script>mermaid.initialize({startOnLoad:true, theme:'default'});</script>
        </body></html>
        """.trimIndent()
    }
    AndroidView(
        modifier = modifier.fillMaxWidth().heightIn(min = 200.dp),
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
        },
        update = { it.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
    )
}
