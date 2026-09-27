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
fun KaTeXView(
    latex: String,
    displayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val html = remember(latex, displayMode) {
        """
        <!DOCTYPE html>
        <html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/katex.min.css">
        <script src="https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/katex.min.js"></script>
        <style>body{margin:0;padding:12px;background:transparent;font-size:16px}</style>
        </head><body>
        <div id="math"></div>
        <script>
          katex.render(`${latex.replace("`", "\\`").replace("\n", " ")}`, document.getElementById('math'), {displayMode: ${displayMode}, throwOnError:false});
        </script>
        </body></html>
        """.trimIndent()
    }
    AndroidView(
        modifier = modifier.fillMaxWidth().heightIn(min = 40.dp),
        factory = {
            WebView(context).apply { settings.javaScriptEnabled = true }
        },
        update = { it.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) }
    )
}
