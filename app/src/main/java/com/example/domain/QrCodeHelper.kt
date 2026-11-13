package com.example.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.WorkplaceQr
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

sealed interface QrValidationResult {
    data class Valid(val workplace: WorkplaceQr) : QrValidationResult
    data class Expired(val workplaceName: String) : QrValidationResult
    data object Inactive : QrValidationResult
    data object UnrecognizedOrUnauthorized : QrValidationResult
}

object QrCodeHelper {
    private const val PREFIX = "NCR_DUTY_QR"

    fun buildQrPayload(workplace: WorkplaceQr): String {
        return "$PREFIX|${workplace.id}|${workplace.qrToken}|${workplace.qrExpiresAtMillis}"
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix[x, y]) Color.parseColor("#0F2744") else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeFromYuv(
        data: ByteArray,
        width: Int,
        height: Int
    ): String? {
        return try {
            val source = PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(bitmap).text
        } catch (_: Exception) {
            null
        }
    }

    fun decodeFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap).text
        } catch (_: Exception) {
            null
        }
    }

    fun validateScannedQr(
        rawQrText: String,
        registeredWorkplaces: List<WorkplaceQr>,
        assignedWorkplaceId: String,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): QrValidationResult {
        val parts = rawQrText.trim().split("|")
        if (parts.size < 3 || parts[0] != PREFIX) {
            return QrValidationResult.UnrecognizedOrUnauthorized
        }
        val workplaceId = parts[1]
        val token = parts[2]

        val matched = registeredWorkplaces.firstOrNull { wp ->
            wp.id == workplaceId && wp.qrToken == token
        } ?: return QrValidationResult.UnrecognizedOrUnauthorized

        if (assignedWorkplaceId.isNotBlank() && matched.id != assignedWorkplaceId) {
            return QrValidationResult.UnrecognizedOrUnauthorized
        }
        if (!matched.isActive) {
            return QrValidationResult.Inactive
        }
        if (matched.qrExpiresAtMillis > 0L && currentTimeMillis > matched.qrExpiresAtMillis) {
            return QrValidationResult.Expired("${matched.companyName} - ${matched.factoryName}")
        }
        return QrValidationResult.Valid(matched)
    }
}
