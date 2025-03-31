package org.autojs.autojs.devplugin.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.intellij.openapi.diagnostic.Logger
import java.awt.image.BufferedImage
import java.util.EnumMap

object QRCodeUtil {
    private val logger = Logger.getInstance(QRCodeUtil::class.java)
    
    /**
     * Generates a QR code image from a text string
     */
    fun generateQRCode(text: String, size: Int = 300): BufferedImage? {
        try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1
            
            val bitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            
            return MatrixToImageWriter.toBufferedImage(bitMatrix)
        } catch (e: Exception) {
            logger.error("Error generating QR code", e)
            return null
        }
    }
} 