package com.nirv.converttopdf.ui.capture

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class CaptureUiState {
    data object Idle    : CaptureUiState()
    data object Loading : CaptureUiState()
    data class  Done(val imageCount: Int) : CaptureUiState()
    data class  Error(val message: String) : CaptureUiState()
}

class CaptureViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    // URIs de la galería del dispositivo cargadas desde MediaStore
    private val _galleryUris = MutableStateFlow<List<Uri>>(emptyList())
    val galleryUris: StateFlow<List<Uri>> = _galleryUris.asStateFlow()

    // URIs seleccionadas por el usuario en el grid
    private val _selectedUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedUris: StateFlow<Set<Uri>> = _selectedUris.asStateFlow()

    // Carga las imágenes del dispositivo desde MediaStore en orden descendente (más recientes primero)
    fun loadGalleryImages(contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            val uris = mutableListOf<Uri>()
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sortOrder  = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id  = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()
                    )
                    uris.add(uri)
                }
            }
            _galleryUris.value = uris
        }
    }

    // Alterna la selección de una URI en el grid
    fun toggleSelection(uri: Uri) {
        val current = _selectedUris.value.toMutableSet()
        if (uri in current) current.remove(uri) else current.add(uri)
        _selectedUris.value = current
    }

    fun clearSelection() {
        _selectedUris.value = emptySet()
    }

    // Confirma la selección: decodifica cada URI seleccionada y la agrega al repositorio
    fun confirmSelection(contentResolver: ContentResolver) {
        val uris = _selectedUris.value.toList()
        if (uris.isEmpty()) return
        _uiState.value = CaptureUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            uris.forEach { uri ->
                contentResolver.openInputStream(uri)
                    ?.use { BitmapFactory.decodeStream(it) }
                    ?.let { imageRepository.addImage(it) }
            }
            val count = imageRepository.getImages().size
            withContext(Dispatchers.Main) {
                _selectedUris.value = emptySet()
                _uiState.value = CaptureUiState.Done(count)
            }
        }
    }

    // Llamado cuando el scanner ML Kit devuelve un bitmap
    fun onBitmapCaptured(bitmap: Bitmap) {
        imageRepository.addImage(bitmap)
        _uiState.value = CaptureUiState.Done(imageRepository.getImages().size)
    }

    fun onError(message: String) {
        _uiState.value = CaptureUiState.Error(message)
    }

    fun resetState() {
        _uiState.value = CaptureUiState.Idle
    }
}
