package com.nirv.converttopdf.ui.preview

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.ImageRepository
import com.nirv.converttopdf.data.repository.DocumentRepository
import com.nirv.converttopdf.domain.usecase.ExportToPdfUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PreviewVM"

sealed class ShareState {
    data object Idle    : ShareState()
    data object Loading : ShareState()
    data class  Ready(val uri: Uri)    : ShareState()
    data class  Error(val msg: String) : ShareState()
}

class PreviewViewModel(
    private val documentId: Long,
    private val documentRepository: DocumentRepository,
    private val imageRepository: ImageRepository,          // mantiene compatibilidad con SignatureScreen
    private val exportToPdfUseCase: ExportToPdfUseCase
) : ViewModel() {

    private val _images       = MutableStateFlow<List<Bitmap>>(emptyList())
    val images: StateFlow<List<Bitmap>> = _images.asStateFlow()

    private val _documentName = MutableStateFlow("")
    val documentName: StateFlow<String> = _documentName.asStateFlow()

    private val _shareState   = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadDocument() }

    private fun loadDocument() {
        Log.d(TAG, "loadDocument: iniciando carga para documentId=$documentId")
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val doc = documentRepository.getDocumentById(documentId)
            Log.d(TAG, "loadDocument: doc=$doc")
            doc?.let { _documentName.value = it.name }

            val bitmaps = documentRepository.getDocumentBitmaps(documentId)
            Log.d(TAG, "loadDocument: ${bitmaps.size} bitmaps cargados → actualizando _images")
            _images.value = bitmaps

            imageRepository.clear()
            bitmaps.forEach { imageRepository.addImage(it) }
            Log.d(TAG, "loadDocument: imageRepository sincronizado")
            _isLoading.value = false
        }
        viewModelScope.launch {
            documentRepository.allDocuments.collect { docs ->
                val found = docs.find { it.id == documentId }
                Log.d(TAG, "allDocuments emit: total=${docs.size}, encontrado=${found?.name}")
                found?.let { _documentName.value = it.name }
            }
        }
    }

    fun removeImage(index: Int) {
        Log.d(TAG, "removeImage: index=$index, total actual=${_images.value.size}")
        viewModelScope.launch {
            documentRepository.deletePage(documentId, index)
            _images.update { current ->
                current.toMutableList().also { it.removeAt(index) }
            }
            imageRepository.removeImage(index)
            Log.d(TAG, "removeImage: completado, imágenes restantes=${_images.value.size}")
        }
    }

    fun renameDocument(name: String) {
        Log.d(TAG, "renameDocument: '$name'")
        viewModelScope.launch {
            documentRepository.renameDocument(documentId, name)
            _documentName.value = name
        }
    }

    fun shareAsPdf() {
        val images = _images.value
        Log.d(TAG, "shareAsPdf: ${images.size} imágenes, nombre='${_documentName.value}'")
        if (images.isEmpty()) return
        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(images, _documentName.value.ifBlank { null })
                .onSuccess { uri ->
                    Log.d(TAG, "shareAsPdf: PDF generado uri=$uri")
                    _shareState.value = ShareState.Ready(uri)
                }
                .onFailure { e ->
                    Log.e(TAG, "shareAsPdf: error al generar PDF", e)
                    _shareState.value = ShareState.Error(e.message ?: "Error al generar PDF")
                }
        }
    }

    fun resetShareState() {
        _shareState.value = ShareState.Idle
    }
}
