package com.nirv.converttopdf.ui.imageedit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.SignatureRepository
import com.nirv.converttopdf.ui.signature.PlacedSignature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "ImageEditVM"

data class PlacedText(
    val id: Int,
    val text: String,
    val offsetX: Float = 80f,
    val offsetY: Float = 200f,
    val fontSize: Float = 18f,
    val color: Color = Color.Black
)

class ImageEditViewModel(
    val pageId: Long,       // id único de DocumentPageEntity
    val imagePath: String,  // path en disco — directo, sin consultar DB
    private val signatureRepository: SignatureRepository
) : ViewModel() {

    val savedSignatures: StateFlow<List<Bitmap>> = signatureRepository.signatures

    private val _placedSignatures = MutableStateFlow<List<PlacedSignature>>(emptyList())
    val placedSignatures: StateFlow<List<PlacedSignature>> = _placedSignatures.asStateFlow()

    private val _placedTexts = MutableStateFlow<List<PlacedText>>(emptyList())
    val placedTexts: StateFlow<List<PlacedText>> = _placedTexts.asStateFlow()

    private val _selectedSignatureId = MutableStateFlow<Int?>(null)
    val selectedSignatureId: StateFlow<Int?> = _selectedSignatureId.asStateFlow()

    private val _selectedTextId = MutableStateFlow<Int?>(null)
    val selectedTextId: StateFlow<Int?> = _selectedTextId.asStateFlow()

    private var nextSignatureId = 0
    private var nextTextId = 0
    private var lastAutoPlacedCount = signatureRepository.signatures.value.size

    init {
        Log.d(TAG, "init: pageId=$pageId, imagePath=$imagePath")
        Log.d(TAG, "File exists=${File(imagePath).exists()}, size=${File(imagePath).length()} bytes")
    }

    fun checkAndAutoPlaceNewSignature() {
        val current = signatureRepository.signatures.value
        if (current.size > lastAutoPlacedCount) {
            current.lastOrNull()?.let { placeSignature(it) }
            lastAutoPlacedCount = current.size
        }
    }

    fun placeSignature(bitmap: Bitmap) {
        val aspectRatio  = (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceAtLeast(0.5f)
        val defaultWidth = 220f
        val defaultHeight = (defaultWidth / aspectRatio).coerceIn(60f, 150f)
        val id = nextSignatureId++
        val offset = _placedSignatures.value.size * 20f
        _placedSignatures.value = _placedSignatures.value + PlacedSignature(
            id       = id,
            bitmap   = bitmap,
            offsetX  = 80f + offset,
            offsetY  = 80f + offset,
            widthDp  = defaultWidth,
            heightDp = defaultHeight
        )
        _selectedSignatureId.value = id
    }

    fun selectSignature(id: Int?) { _selectedSignatureId.value = id }
    fun deselectAll() { _selectedSignatureId.value = null; _selectedTextId.value = null }

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
        if (_selectedSignatureId.value == id) _selectedSignatureId.value = null
    }

    fun placeText(text: String) {
        if (text.isBlank()) return
        val id = nextTextId++
        val offset = _placedTexts.value.size * 30f
        _placedTexts.value = _placedTexts.value + PlacedText(
            id = id, text = text, offsetX = 80f + offset, offsetY = 300f + offset
        )
        _selectedTextId.value = id
    }

    fun placeDateText() {
        val date = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        placeText(date)
    }

    fun selectText(id: Int?) { _selectedTextId.value = id }

    fun moveText(id: Int, dx: Float, dy: Float) {
        _placedTexts.value = _placedTexts.value.map { t ->
            if (t.id == id) t.copy(offsetX = t.offsetX + dx, offsetY = t.offsetY + dy) else t
        }
    }

    fun updateText(id: Int, newText: String? = null, fontSize: Float? = null, color: Color? = null) {
        _placedTexts.value = _placedTexts.value.map { t ->
            if (t.id == id) t.copy(
                text     = newText   ?: t.text,
                fontSize = fontSize  ?: t.fontSize,
                color    = color     ?: t.color
            ) else t
        }
    }

    fun removePlacedText(id: Int) {
        _placedTexts.value = _placedTexts.value.filter { it.id != id }
        if (_selectedTextId.value == id) _selectedTextId.value = null
    }

    fun confirmAndSave(imageDisplaySize: IntSize, screenDensity: Float, onDone: () -> Unit) {
        Log.d(TAG, "confirmAndSave: pageId=$pageId, path=$imagePath, displaySize=$imageDisplaySize")
        viewModelScope.launch(Dispatchers.IO) {
            val original = BitmapFactory.decodeFile(imagePath) ?: run {
                Log.e(TAG, "confirmAndSave: no se pudo decodificar $imagePath")
                return@launch
            }
            Log.d(TAG, "confirmAndSave: original ${original.width}x${original.height}")

            val result = original.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val scaleX = original.width.toFloat() / imageDisplaySize.width.coerceAtLeast(1)
            val scaleY = original.height.toFloat() / imageDisplaySize.height.coerceAtLeast(1)

            _placedSignatures.value.forEach { placed ->
                val bitmapX     = placed.offsetX * scaleX
                val bitmapY     = placed.offsetY * scaleY
                val sigWidthPx  = (placed.widthDp * screenDensity * scaleX).toInt().coerceAtLeast(1)
                val sigHeightPx = (placed.heightDp * screenDensity * scaleY).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(placed.bitmap, sigWidthPx, sigHeightPx, true)
                val paint = Paint().apply {
                    isAntiAlias = true
                    if (placed.tintColor != Color.Black) {
                        colorFilter = PorterDuffColorFilter(
                            placed.tintColor.toArgb(), PorterDuff.Mode.SRC_IN
                        )
                    }
                }
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
            }

            _placedTexts.value.forEach { placed ->
                val textSizePx = placed.fontSize * screenDensity * scaleX
                val paint = Paint().apply {
                    isAntiAlias = true
                    textSize    = textSizePx
                    color       = placed.color.toArgb()
                }
                canvas.drawText(
                    placed.text,
                    placed.offsetX * scaleX,
                    placed.offsetY * scaleY + textSizePx,
                    paint
                )
            }

            File(imagePath).outputStream().use { out ->
                result.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Log.d(TAG, "confirmAndSave: guardado en $imagePath")

            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
