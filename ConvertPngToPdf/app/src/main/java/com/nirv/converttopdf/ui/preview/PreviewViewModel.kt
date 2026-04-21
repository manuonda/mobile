package com.nirv.converttopdf.ui.preview

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.repository.DocumentRepository
import com.nirv.converttopdf.domain.usecase.ExportToPdfUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val exportToPdfUseCase: ExportToPdfUseCase
) : ViewModel() {

    private val _pages        = MutableStateFlow<List<String>>(emptyList())
    val pages: StateFlow<List<String>> = _pages.asStateFlow()

    private val _documentName = MutableStateFlow("")
    val documentName: StateFlow<String> = _documentName.asStateFlow()

    private val _shareState   = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    private val _isLoading    = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observePages()
        observeDocumentName()
    }

    private fun observePages() {
        viewModelScope.launch(Dispatchers.IO) {
            documentRepository.getDocumentPathsFlow(documentId).collect { paths ->
                Log.d(TAG, "observePages: ${paths.size} páginas")
                _pages.value = paths
                _isLoading.value = false
            }
        }
    }

    private fun observeDocumentName() {
        viewModelScope.launch {
            documentRepository.allDocuments.collect { docs ->
                docs.find { it.id == documentId }?.let { _documentName.value = it.name }
            }
        }
    }

    fun removePage(index: Int) {
        Log.d(TAG, "removePage: index=$index")
        viewModelScope.launch(Dispatchers.IO) {
            documentRepository.deletePage(documentId, index)
            // Room Flow emite automáticamente tras el delete — no hace falta recargar manualmente
        }
    }

    fun renameDocument(name: String) {
        viewModelScope.launch {
            documentRepository.renameDocument(documentId, name)
            _documentName.value = name
        }
    }

    fun shareAsPdf() {
        val paths = _pages.value
        if (paths.isEmpty()) return
        Log.d(TAG, "shareAsPdf: ${paths.size} páginas")
        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(paths, _documentName.value.ifBlank { null })
                .onSuccess { uri ->
                    Log.d(TAG, "shareAsPdf: uri=$uri")
                    _shareState.value = ShareState.Ready(uri)
                }
                .onFailure { e ->
                    Log.e(TAG, "shareAsPdf: error", e)
                    _shareState.value = ShareState.Error(e.message ?: "Error al generar PDF")
                }
        }
    }

    fun resetShareState() {
        _shareState.value = ShareState.Idle
    }
}
