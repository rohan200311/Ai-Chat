package com.aichat.app.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeUtil @Inject constructor() {

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun parseProviderImport(qrContent: String): Result<String> {
        // QR contains JSON of providers, optionally encrypted
        return try {
            // Basic validation
            if (qrContent.trim().startsWith("{") || qrContent.trim().startsWith("[")) {
                Result.success(qrContent)
            } else {
                // Try base64 decode
                val decoded = String(android.util.Base64.decode(qrContent, android.util.Base64.DEFAULT))
                Result.success(decoded)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
