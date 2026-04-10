package com.nirv.scansheet.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class MlKitOcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun process(bitmap: Bitmap): List<String> {
        val processed = preprocessBitmap(bitmap)
        val image = InputImage.fromBitmap(processed, 0)
        val result = recognizer.process(image).await()

        return result.textBlocks
            .flatMap { block -> block.lines }
            .map { line -> line.text.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Preprocesa el bitmap para mejorar el reconocimiento en documentos manuscritos:
     * 1. Escala de grises: elimina interferencia de colores (resaltadores, tinta de color)
     * 2. Aumento de contraste: hace el texto más nítido sobre el fondo
     */
    private fun preprocessBitmap(original: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()

        // Paso 1: escala de grises (saturation = 0)
        val grayscale = ColorMatrix()
        grayscale.setSaturation(0f)

        // Paso 2: aumentar contraste (escala los valores de color alejándolos del gris medio)
        // Valor 1.8f = 80% más contraste. Rango recomendado: 1.5 – 2.5
        val contrast = 1.8f
        val brightness = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        // Aplica primero grises, luego contraste
        colorMatrix.postConcat(grayscale)
        colorMatrix.postConcat(contrastMatrix)

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return result
    }
}
