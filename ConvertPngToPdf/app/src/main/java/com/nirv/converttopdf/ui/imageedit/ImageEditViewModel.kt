package com.nirv.converttopdf.ui.imageedit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import com.nirv.converttopdf.data.db.entity.DocumentPageEntity
import com.nirv.converttopdf.ui.signature.PlacedSignature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import java.io.File

enum class ImageFilter(val label: String) {
    ORIGINAL("Original"),
    VINTAGE("Vintage"),
    VIVID("Vívido"),
    MONO("Mono"),
    ENHANCE("Mejorar"),
    COLD("Frío")
}

fun ImageFilter.toComposeColorFilter(): androidx.compose.ui.graphics.ColorFilter? = when (this) {
    ImageFilter.ORIGINAL -> null
    ImageFilter.VINTAGE  -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            0.9f, 0.05f, 0.05f, 0f, 0.08f,
            0.05f, 0.8f, 0.05f, 0f, 0.04f,
            0f,   0f,   0.6f,  0f,  0f,
            0f,   0f,   0f,    1f,  0f
        ))
    )
    ImageFilter.VIVID    -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(2.2f) }
    )
    ImageFilter.MONO     -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
    )
    ImageFilter.ENHANCE  -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            1.2f, 0f,   0f,   0f, 0.04f,
            0f,   1.2f, 0f,   0f, 0.04f,
            0f,   0f,   1.2f, 0f, 0.04f,
            0f,   0f,   0f,   1f,  0f
        ))
    )
    ImageFilter.COLD     -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            0.85f, 0f,    0f,    0f,  0f,
            0f,    0.9f,  0f,    0f,  0f,
            0f,    0f,    1.25f, 0f,  0.08f,
            0f,    0f,    0f,    1f,  0f
        ))
    )
}

fun ImageFilter.toAndroidColorFilter(): ColorMatrixColorFilter? = when (this) {
    ImageFilter.ORIGINAL -> null
    ImageFilter.VINTAGE  -> ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
        0.9f, 0.05f, 0.05f, 0f, 20f,
        0.05f, 0.8f, 0.05f, 0f, 10f,
        0f,   0f,   0.6f,  0f,  0f,
        0f,   0f,   0f,    1f,  0f
    )))
    ImageFilter.VIVID    -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(2.2f) })
    ImageFilter.MONO     -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    ImageFilter.ENHANCE  -> ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
        1.2f, 0f,   0f,   0f, 10f,
        0f,   1.2f, 0f,   0f, 10f,
        0f,   0f,   1.2f, 0f, 10f,
        0f,   0f,   0f,   1f,  0f
    )))
    ImageFilter.COLD     -> ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
        0.85f, 0f,    0f,    0f,  0f,
        0f,    0.9f,  0f,    0f,  0f,
        0f,    0f,    1.25f, 0f, 20f,
        0f,    0f,    0f,    1f,  0f
    )))
}

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
    val initialPageId: Long,
    val initialImagePath: String,
    val allPages: List<DocumentPageEntity>,
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

    private val _currentIndex = MutableStateFlow(allPages.indexOfFirst { it.id == initialPageId }.coerceAtLeast(0))
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Reactive current page properties
    val currentImagePath: StateFlow<String> = _currentIndex.map { index ->
        allPages.getOrNull(index)?.imagePath ?: initialImagePath
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialImagePath)

    val currentPageId: StateFlow<Long> = _currentIndex.map { index ->
        allPages.getOrNull(index)?.id ?: initialPageId
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialPageId)

    val totalPages: Int get() = allPages.size

    private val _imageVersion = MutableStateFlow(0L)
    val imageVersion: StateFlow<Long> = _imageVersion.asStateFlow()

    init {
        Log.d(TAG, "init: initialPageId=$initialPageId, totalPages=${allPages.size}")
        viewModelScope.launch(Dispatchers.IO) {
            val backup = File("$initialImagePath.bak")
            if (!backup.exists()) {
                File(initialImagePath).copyTo(backup, overwrite = false)
            }
        }
    }

    fun resetToOriginal() {
        viewModelScope.launch(Dispatchers.IO) {
            val backup = File("$initialImagePath.bak")
            if (backup.exists()) {
                backup.copyTo(File(currentImagePath.value), overwrite = true)
                withContext(Dispatchers.Main) { _imageVersion.value++ }
            }
        }
    }

    fun rotateImage(degrees: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = currentImagePath.value
            val bmp = BitmapFactory.decodeFile(path) ?: return@launch
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            File(path).outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            withContext(Dispatchers.Main) { _imageVersion.value++ }
        }
    }

    fun flipImage(horizontal: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = currentImagePath.value
            val bmp = BitmapFactory.decodeFile(path) ?: return@launch
            val matrix = Matrix().apply {
                if (horizontal) postScale(-1f, 1f, bmp.width / 2f, bmp.height / 2f)
                else postScale(1f, -1f, bmp.width / 2f, bmp.height / 2f)
            }
            val flipped = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            File(path).outputStream().use { flipped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            withContext(Dispatchers.Main) { _imageVersion.value++ }
        }
    }

    fun applyFilterAndSave(filter: ImageFilter) {
        if (filter == ImageFilter.ORIGINAL) {
            _imageVersion.value++
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val path = currentImagePath.value
            val original = BitmapFactory.decodeFile(path) ?: return@launch
            val result = original.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val paint = Paint().apply { colorFilter = filter.toAndroidColorFilter() }
            canvas.drawBitmap(original, 0f, 0f, paint)
            File(path).outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            withContext(Dispatchers.Main) { _imageVersion.value++ }
        }
    }

    fun goToPage(index: Int) {
        if (index in allPages.indices && index != _currentIndex.value) {
            _currentIndex.value = index
            _placedSignatures.value = emptyList()
            _placedTexts.value = emptyList()
            _selectedSignatureId.value = null
            _selectedTextId.value = null
        }
    }

    fun syncInitialPage(pages: List<DocumentPageEntity>) {
        if (pages.isEmpty()) return
        val correctIndex = pages.indexOfFirst { it.id == initialPageId }
        if (correctIndex >= 0 && correctIndex != _currentIndex.value) {
            _currentIndex.value = correctIndex
        }
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
        val path = currentImagePath.value
        Log.d(TAG, "confirmAndSave: path=$path, displaySize=$imageDisplaySize")
        viewModelScope.launch(Dispatchers.IO) {
            val original = BitmapFactory.decodeFile(path) ?: run {
                Log.e(TAG, "confirmAndSave: no se pudo decodificar $path")
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

            File(path).outputStream().use { out ->
                result.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Log.d(TAG, "confirmAndSave: guardado en $path")

            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
