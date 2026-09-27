package com.aichat.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.aichat.app.domain.model.Attachment
import com.aichat.app.domain.model.AttachmentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun parseUri(uri: Uri): Attachment = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = getFileName(uri) ?: "file"
        val size = getFileSize(uri)
        val type = when {
            mime.startsWith("image/") -> AttachmentType.IMAGE
            mime == "application/pdf" || name.endsWith(".pdf", true) -> AttachmentType.PDF
            mime.contains("word") || name.endsWith(".docx", true) -> AttachmentType.DOCX
            mime.startsWith("text/") -> AttachmentType.TEXT
            else -> AttachmentType.OTHER
        }

        var base64: String? = null
        var extracted: String? = null

        if (type == AttachmentType.IMAGE) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            }
        } else {
            extracted = when (type) {
                AttachmentType.PDF -> parsePdf(uri)
                AttachmentType.DOCX -> parseDocx(uri)
                AttachmentType.TEXT -> parseText(uri)
                else -> null
            }
        }

        Attachment(
            type = type,
            name = name,
            mimeType = mime,
            uri = uri.toString(),
            base64Data = base64,
            extractedText = extracted,
            size = size
        )
    }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) return cursor.getString(nameIndex)
        }
        return uri.lastPathSegment
    }

    private fun getFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex != -1) return cursor.getLong(sizeIndex)
        }
        return 0
    }

    private fun parsePdf(uri: Uri): String? = try {
        // Using PdfBox Android (com.tom-roush:pdfbox-android)
        context.contentResolver.openInputStream(uri)?.use { input ->
            val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(input)
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            val text = stripper.getText(doc)
            doc.close()
            text.take(20000) // limit
        }
    } catch (e: Exception) { "Failed to parse PDF: ${e.message}" }

    private fun parseDocx(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val doc = org.apache.poi.xwpf.usermodel.XWPFDocument(input)
            val text = doc.paragraphs.joinToString("\n") { it.text }
            doc.close()
            text.take(20000)
        }
    } catch (e: Exception) { "Failed to parse DOCX: ${e.message}" }

    private fun parseText(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText().take(20000) }
    } catch (_: Exception) { null }
}
