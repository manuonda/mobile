package com.nirv.converttopdf.ui.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import com.nirv.converttopdf.data.ImageRepository
import com.nirv.converttopdf.data.SignatureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Representa la firma colocada sobre el documento como overlay arrastrable.
//   offsetX / offsetY → posición en píxeles de pantalla
//   widthDp / heightDp → tamaño en dp (para que Modifier.size() sea consistente)
//   tintColor → color de tinta (aplicado con ColorFilter al mostrar y al bake)
data class PlacedSignature(
    val bitmap: Bitmap,
    val offsetX: Float  = 80f,
    val offsetY: Float  = 80f,
    val widthDp: Float  = 160f,
    val heightDp: Float = 80f,
    val tintColor: Color = Color.Black
)

class SignatureViewModel(
    private val imageRepository: ImageRepository,
    private val signatureRepository: SignatureRepository
) : ViewModel() {

    val images: StateFlow<List<Bitmap>>  = imageRepository.images
    val savedSignatures: StateFlow<List<Bitmap>> = signatureRepository.signatures

    private val _placedSignature = MutableStateFlow<PlacedSignature?>(null)
    val placedSignature: StateFlow<PlacedSignature?> = _placedSignature.asStateFlow()

    fun placeSignature(bitmap: Bitmap) {
        // Calcular el tamaño inicial del overlay respetando el aspect ratio del bitmap.
        // Si la firma es ancha (ej. 300×80px → ratio 3.75), el overlay será 220×58dp.
        // Si es casi cuadrada (ej. 100×80px → ratio 1.25), será 220×176dp, pero
        // lo limitamos a 150dp de alto para no cubrir demasiado el documento.
        val aspectRatio = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceAtLeast(0.5f)
        val defaultWidth  = 220f
        val defaultHeight = (defaultWidth / aspectRatio).coerceIn(60f, 150f)
        _placedSignature.value = PlacedSignature(
            bitmap    = bitmap,
            widthDp   = defaultWidth,
            heightDp  = defaultHeight
        )
    }

    // Mueve el overlay (drag en la zona central de la firma)
    fun moveSignature(dx: Float, dy: Float) {
        val c = _placedSignature.value ?: return
        _placedSignature.value = c.copy(offsetX = c.offsetX + dx, offsetY = c.offsetY + dy)
    }

    // Redimensiona arrastrando la esquina inferior-derecha
    // dWidthDp / dHeightDp vienen de dragAmount convertido a dp en la Screen
    fun resizeSignature(dWidthDp: Float, dHeightDp: Float) {
        val c = _placedSignature.value ?: return
        _placedSignature.value = c.copy(
            widthDp  = (c.widthDp  + dWidthDp).coerceAtLeast(60f),
            heightDp = (c.heightDp + dHeightDp).coerceAtLeast(40f)
        )
    }

    // Cambia el color de tinta del overlay (panel "Editar")
    fun updateSignatureColor(color: Color) {
        val c = _placedSignature.value ?: return
        _placedSignature.value = c.copy(tintColor = color)
    }

    fun removePlacedSignature() {
        _placedSignature.value = null
    }

    // Bake: dibuja la firma sobre el último bitmap en su posición y tamaño actuales.
    // imageLayoutSize = tamaño en px del widget Image() en pantalla (para escalar).
    fun applySignatureAtPosition(imageLayoutSize: IntSize, screenDensity: Float) {
        val placed = _placedSignature.value ?: return
        val images = imageRepository.getImages()
        if (images.isEmpty()) return

        val lastIndex = images.lastIndex
        val original  = images[lastIndex]

        val scaleX = original.width.toFloat()  / imageLayoutSize.width.coerceAtLeast(1)
        val scaleY = original.height.toFloat() / imageLayoutSize.height.coerceAtLeast(1)

        val bitmapX   = placed.offsetX * scaleX
        val bitmapY   = placed.offsetY * scaleY
        val sigWidthPx  = (placed.widthDp  * screenDensity * scaleX).toInt().coerceAtLeast(1)
        val sigHeightPx = (placed.heightDp * screenDensity * scaleY).toInt().coerceAtLeast(1)

        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val scaled = Bitmap.createScaledBitmap(placed.bitmap, sigWidthPx, sigHeightPx, true)

        val paint = Paint().apply {
            isAntiAlias = true
            // Aplica el color de tinta elegido por el usuario
            if (placed.tintColor != Color.Black) {
                colorFilter = PorterDuffColorFilter(
                    placed.tintColor.toArgb(),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }
        canvas.drawBitmap(scaled, bitmapX, bitmapY, paint)

        imageRepository.removeImage(lastIndex)
        imageRepository.addImage(result)
        _placedSignature.value = null
    }
}
