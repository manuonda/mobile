package com.nirv.converttopdf.ui.preview

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.ImageRepository
import com.nirv.converttopdf.data.repository.DocumentRepository
import com.nirv.converttopdf.domain.usecase.ExportToPdfUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    init { loadDocument() }

    private fun loadDocument() {
        viewModelScope.launch {
            // Carga bitmaps desde disco
            val bitmaps = documentRepository.getDocumentBitmaps(documentId)
            _images.value = bitmaps
            // Sincroniza al ImageRepository para que SignatureScreen pueda usarlos
            imageRepository.clear()
            bitmaps.forEach { imageRepository.addImage(it) }
        }
        // Observa el nombre del documento reactivamente
        viewModelScope.launch {
            documentRepository.allDocuments.collect { docs ->
                docs.find { it.id == documentId }?.let { _documentName.value = it.name }
            }
        }
    }

    fun removeImage(index: Int) {
        viewModelScope.launch {
            documentRepository.deletePage(documentId, index)
            _images.update { current ->
                current.toMutableList().also { it.removeAt(index) }
            }
            imageRepository.removeImage(index)
        }
    }

    fun renameDocument(name: String) {
        viewModelScope.launch {
            documentRepository.renameDocument(documentId, name)
            _documentName.value = name
        }
    }

    fun shareAsPdf() {
        val images = _images.value
        if (images.isEmpty()) return
        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(images, _documentName.value.ifBlank { null })
                .onSuccess { uri -> _shareState.value = ShareState.Ready(uri) }
                .onFailure { e  -> _shareState.value = ShareState.Error(e.message ?: "Error al generar PDF") }
        }
    }

    fun resetShareState() {
        _shareState.value = ShareState.Idle
    }
}
