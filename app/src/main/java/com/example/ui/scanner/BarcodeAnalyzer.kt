package com.example.ui.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.QR_CODE
            ),
            DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }

    private var isScanning = true

    fun resumeScanning() {
        isScanning = true
    }

    fun pauseScanning() {
        isScanning = false
    }

    override fun analyze(image: ImageProxy) {
        if (!isScanning) {
            image.close()
            return
        }

        val buffer: ByteBuffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val width = image.width
        val height = image.height

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

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decodeWithState(binaryBitmap)
            val codeText = result.text?.trim()
            if (!codeText.isNullOrBlank()) {
                isScanning = false
                onBarcodeDetected(codeText)
            }
        } catch (_: NotFoundException) {
            // Normal when no barcode in current frame
        } catch (e: Exception) {
            // Ignore frame decode exceptions
        } finally {
            reader.reset()
            image.close()
        }
    }
}
