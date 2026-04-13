package com.nirv.converttopdf.ui.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
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

// Información de layout de cada imagen en el canvas scrollable (en px de pantalla)
data class ImageLayoutInfo(
    val topY: Float,           // y desde el tope del canvas
    val displaySize: IntSize   // tamaño del widget en pantalla (px)
)

// Firma flotante posicionada en el canvas continuo.
//   offsetX/Y → posición absoluta en el canvas (px de pantalla), no relativa a ninguna imagen.
//   rotation  → rotación en grados (0 = sin rotación)
data class PlacedSignature(
    val id: Int,
    val bitmap: Bitmap,
    val offsetX: Float   = 80f,
    val offsetY: Float   = 80f,
    val widthDp: Float   = 160f,
    val heightDp: Float  = 80f,
    val rotation: Float  = 0f,
    val tintColor: Color = Color.Black
)

class SignatureViewModel(
    private val imageRepository: ImageRepository,
    private val signatureRepository: SignatureRepository
) : ViewModel() {

    val images: StateFlow<List<Bitmap>>          = imageRepository.images
    val savedSignatures: StateFlow<List<Bitmap>> = signatureRepository.signatures

    private val _placedSignatures = MutableStateFlow<List<PlacedSignature>>(emptyList())
    val placedSignatures: StateFlow<List<PlacedSignature>> = _placedSignatures.asStateFlow()

    private val _selectedId = MutableStateFlow<Int?>(null)
    val selectedId: StateFlow<Int?> = _selectedId.asStateFlow()

    // Inicializado con el tamaño actual → no auto-place firmas preexistentes al entrar
    private var lastAutoPlacedCount = signatureRepository.signatures.value.size

    private var nextId = 0

    // Coloca automáticamente la firma recién dibujada cerca del área visible actual.
    // scrollOffsetPx = valor actual de ScrollState.value (px desde el tope del canvas)
    fun checkAndAutoPlaceNewSignatures(scrollOffsetPx: Int) {
        val current = signatureRepository.signatures.value
        if (current.size > lastAutoPlacedCount) {
            current.lastOrNull()?.let {
                placeSignature(it, initialY = scrollOffsetPx.toFloat() + 120f)
            }
            lastAutoPlacedCount = current.size
        }
    }

    // Agrega una firma al canvas y la selecciona
    fun placeSignature(bitmap: Bitmap, initialY: Float = 80f) {
        val aspectRatio   = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceAtLeast(0.5f)
        val defaultWidth  = 220f
        val defaultHeight = (defaultWidth / aspectRatio).coerceIn(60f, 150f)
        val id = nextId++
        val offset = _placedSignatures.value.size * 20f   // escalonado para múltiples firmas
        _placedSignatures.value = _placedSignatures.value + PlacedSignature(
            id      = id,
            bitmap  = bitmap,
            offsetX = 80f + offset,
            offsetY = initialY + offset,
            widthDp = defaultWidth,
            heightDp = defaultHeight
        )
        _selectedId.value = id
    }

    fun selectSignature(id: Int?) { _selectedId.value = id }
    fun deselectAll()             { _selectedId.value = null }

    fun moveSignature(id: Int, dx: Float, dy: Float) {
        _placedSignatures.value = _placedSignatures.value.map { s ->
            if (s.id == id) s.copy(offsetX = s.offsetX + dx, offsetY = s.offsetY + dy) else s
        }
    }

    fun resizeSignature(id: Int, dWidthDp: Float, dHeightDp: Float) {
        _placedSignatures.value = _placedSignatures.value.map { s ->
            if (s.id == id) s.copy(
                widthDp  = (s.widthDp  + dWidthDp).coerceAtLeast(60f),
                heightDp = (s.heightDp + dHeightDp).coerceAtLeast(40f)
            ) else s
        }
    }

    // Rota la firma: deltaDegrees viene del drag horizontal del handle ↻
    // (positivo = sentido horario, negativo = antihorario)
    fun rotateSignature(id: Int, deltaDegrees: Float) {
        _placedSignatures.value = _placedSignatures.value.map { s ->
            if (s.id == id) s.copy(rotation = (s.rotation + deltaDegrees) % 360f) else s
        }
    }

    fun updateSignatureColor(id: Int, color: Color) {
        _placedSignatures.value = _placedSignatures.value.map { s ->
            if (s.id == id) s.copy(tintColor = color) else s
        }
    }

    fun removePlacedSignature(id: Int) {
        _placedSignatures.value = _placedSignatures.value.filter { it.id != id }
        if (_selectedId.value == id) _selectedId.value = null
    }

    // Bake: para cada firma, determina sobre qué imagen cae según su posición Y
    // en el canvas y dibuja la firma en el bitmap correspondiente.
    //
    // imageLayouts: mapa de índice de imagen → ImageLayoutInfo (posición en canvas + tamaño)
    fun applyAllSignatures(
        imageLayouts: Map<Int, ImageLayoutInfo>,
        screenDensity: Float
    ) {
        val signatures = _placedSignatures.value
        if (signatures.isEmpty()) return

        val images = imageRepository.getImages()

        signatures.forEach { placed ->
            // Encuentra la imagen que contiene el punto superior-izquierdo de la firma
            val targetIndex = imageLayouts.entries.firstOrNull { (_, layout) ->
                placed.offsetY >= layout.topY &&
                placed.offsetY < layout.topY + layout.displaySize.height
            }?.key ?: return@forEach

            val layout   = imageLayouts[targetIndex] ?: return@forEach
            val original = images.getOrNull(targetIndex) ?: return@forEach

            val scaleX = original.width.toFloat()  / layout.displaySize.width.coerceAtLeast(1)
            val scaleY = original.height.toFloat() / layout.displaySize.height.coerceAtLeast(1)

            // offsetY relativo al tope de la imagen (canvas Y − imagen topY)
            val relativeY = placed.offsetY - layout.topY

            val bitmapX     = placed.offsetX * scaleX
            val bitmapY     = relativeY * scaleY
            val sigWidthPx  = (placed.widthDp  * screenDensity * scaleX).toInt().coerceAtLeast(1)
            val sigHeightPx = (placed.heightDp * screenDensity * scaleY).toInt().coerceAtLeast(1)

            val result = original.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val scaled = Bitmap.createScaledBitmap(placed.bitmap, sigWidthPx, sigHeightPx, true)
            val paint  = Paint().apply {
                isAntiAlias = true
                if (placed.tintColor != Color.Black) {
                    colorFilter = PorterDuffColorFilter(
                        placed.tintColor.toArgb(), PorterDuff.Mode.SRC_IN
                    )
                }
            }
            // Aplica rotación alrededor del centro de la firma durante el bake
            if (placed.rotation != 0f) {
                val cx = bitmapX + sigWidthPx / 2f
                val cy = bitmapY + sigHeightPx / 2f
                val matrix = Matrix().apply { postRotate(placed.rotation, cx, cy) }
                canvas.save()
                canvas.concat(matrix)
                canvas.drawBitmap(scaled, bitmapX, bitmapY, paint)
                canvas.restore()
            } else {
                canvas.drawBitmap(scaled, bitmapX, bitmapY, paint)
            }
            imageRepository.replaceImage(targetIndex, result)
        }

        _placedSignatures.value = emptyList()
        _selectedId.value = null
    }
}
