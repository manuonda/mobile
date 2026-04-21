package com.nirv.converttopdf.ui.capture

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class CaptureUiState {
    data object Idle    : CaptureUiState()
    data object Loading : CaptureUiState()
    data class  Done(val documentId: Long) : CaptureUiState()
    data class  Error(val message: String) : CaptureUiState()
}

private const val TAG = "CaptureVM"

class CaptureViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _galleryUris = MutableStateFlow<List<Uri>>(emptyList())
    val galleryUris: StateFlow<List<Uri>> = _galleryUris.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedUris: StateFlow<Set<Uri>> = _selectedUris.asStateFlow()

    private val _isGalleryLoading = MutableStateFlow(false)
    val isGalleryLoading: StateFlow<Boolean> = _isGalleryLoading.asStateFlow()

    fun loadGalleryImages(contentResolver: ContentResolver) {
        if (_isGalleryLoading.value) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isGalleryLoading.value = true
            val uris = mutableListOf<Uri>()
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            
            try {
                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, 
                    null, 
                    null, 
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (cursor.moveToNext() && uris.size < 100) {
                        val id = cursor.getLong(idCol)
                        uris.add(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()))
                    }
                }
                _galleryUris.value = uris
            } catch (e: Exception) {
                _uiState.value = CaptureUiState.Error("Error al cargar galería: ${e.message}")
            } finally {
                _isGalleryLoading.value = false
            }
        }
    }

    fun toggleSelection(uri: Uri) {
        val current = _selectedUris.value.toMutableSet()
        if (uri in current) current.remove(uri) else current.add(uri)
        _selectedUris.value = current
    }

    fun clearSelection() {
        _selectedUris.value = emptySet()
    }

    fun confirmSelection(contentResolver: ContentResolver, documentName: String) {
        val uris = _selectedUris.value.toList()
        Log.d(TAG, "confirmSelection: ${uris.size} uris seleccionadas, nombre='$documentName'")
        if (uris.isEmpty()) return
        _uiState.value = CaptureUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val bitmaps = uris.mapNotNull { uri ->
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    .also { bmp -> Log.d(TAG, "  uri=$uri → bitmap=${if (bmp != null) "${bmp.width}x${bmp.height}" else "NULL"}") }
            }
            Log.d(TAG, "confirmSelection: ${bitmaps.size}/${uris.size} bitmaps decodificados")
            val docId = documentRepository.createDocument(documentName, bitmaps)
            Log.d(TAG, "confirmSelection: documento creado con id=$docId")
            withContext(Dispatchers.Main) {
                _selectedUris.value = emptySet()
                _uiState.value = CaptureUiState.Done(docId)
            }
        }
    }

    fun addToExistingDocument(contentResolver: ContentResolver, documentId: Long) {
        val uris = _selectedUris.value.toList()
        Log.d(TAG, "addToExistingDocument: ${uris.size} uris para docId=$documentId")
        if (uris.isEmpty()) return
        _uiState.value = CaptureUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val bitmaps = uris.mapNotNull { uri ->
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    .also { bmp -> Log.d(TAG, "  uri=$uri → bitmap=${if (bmp != null) "${bmp.width}x${bmp.height}" else "NULL"}") }
            }
            Log.d(TAG, "addToExistingDocument: ${bitmaps.size}/${uris.size} bitmaps decodificados")
            documentRepository.addPagesToDocument(documentId, bitmaps)
            Log.d(TAG, "addToExistingDocument: páginas añadidas a docId=$documentId")
            withContext(Dispatchers.Main) {
                _selectedUris.value = emptySet()
                _uiState.value = CaptureUiState.Done(documentId)
            }
        }
    }

    fun onBitmapCaptured(
        bitmap: android.graphics.Bitmap,
        documentName: String? = null,
        documentId: Long? = null
    ) {
        Log.d(TAG, "onBitmapCaptured: ${bitmap.width}x${bitmap.height}, docId=$documentId, nombre=$documentName")
        _uiState.value = CaptureUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val docId = when {
                documentId != null -> {
                    documentRepository.addPagesToDocument(documentId, listOf(bitmap))
                    Log.d(TAG, "onBitmapCaptured: página añadida a docId=$documentId")
                    documentId
                }
                else -> {
                    val name = documentName ?: "CamScanner_${System.currentTimeMillis()}"
                    val id = documentRepository.createDocument(name, listOf(bitmap))
                    Log.d(TAG, "onBitmapCaptured: documento creado con id=$id, nombre='$name'")
                    id
                }
            }
            withContext(Dispatchers.Main) {
                _uiState.value = CaptureUiState.Done(docId)
            }
        }
    }

    fun onError(message: String) {
        _uiState.value = CaptureUiState.Error(message)
    }

    fun resetState() {
        _uiState.value = CaptureUiState.Idle
    }
}
