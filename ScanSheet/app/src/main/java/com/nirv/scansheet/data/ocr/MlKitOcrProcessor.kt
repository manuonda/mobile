package com.nirv.scansheet.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Procesa un Bitmap con ML Kit y devuelve las líneas de texto reconocidas.
 * Usa la variante bundled (modelo incluido en el APK, sin internet).
 */
class MlKitOcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * @param bitmap Imagen capturada por CameraX
     * @return Lista de líneas de texto reconocidas (puede estar vacía)
     */
    suspend fun process(bitmap: Bitmap): List<String> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()

        // Cada TextBlock es un bloque, cada Line es una línea dentro del bloque
        return result.textBlocks
            .flatMap { block -> block.lines }
            .map { line -> line.text.trim() }
            .filter { it.isNotBlank() }
    }
}
