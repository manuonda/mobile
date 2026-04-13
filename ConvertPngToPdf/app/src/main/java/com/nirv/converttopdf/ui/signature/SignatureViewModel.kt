package com.nirv.converttopdf.ui.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import com.nirv.converttopdf.data.ImageRepository

class SignatureViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {

    // Recibe el bitmap de la firma ya renderizada desde la Screen
    // y la superpone sobre la última imagen del repositorio.
    // El ViewModel no conoce nada de Compose — solo trabaja con Bitmaps.
    fun applySignature(signatureBitmap: Bitmap) {
        val images = imageRepository.getImages()
        if (images.isEmpty()) return

        val lastIndex = images.lastIndex
        val original = images[lastIndex]

        // Copiamos el bitmap original en modo mutable para dibujar sobre él
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Escalamos la firma al tamaño del documento para que encaje correctamente
        val scaled = Bitmap.createScaledBitmap(
            signatureBitmap,
            original.width,
            original.height,
            true
        )

        // Dibujamos la firma encima del documento con leve transparencia
        val paint = Paint().apply { alpha = 220 }
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        // Reemplazamos la imagen en el repositorio con la versión firmada
        imageRepository.removeImage(lastIndex)
        imageRepository.addImage(result)
    }
}
