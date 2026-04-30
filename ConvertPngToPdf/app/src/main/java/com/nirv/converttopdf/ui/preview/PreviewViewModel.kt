package com.nirv.converttopdf.ui.preview

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.db.entity.DocumentPageEntity
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

    // Expone entidades completas: cada página tiene id, imagePath, pageOrder
    private val _pages = MutableStateFlow<List<DocumentPageEntity>>(emptyList())
    val pages: StateFlow<List<DocumentPageEntity>> = _pages.asStateFlow()

    private val _documentName = MutableStateFlow("")
    val documentName: StateFlow<String> = _documentName.asStateFlow()

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observePages()
        observeDocumentName()
    }

    private fun observePages() {
        viewModelScope.launch(Dispatchers.IO) {
            documentRepository.getDocumentPagesFlow(documentId).collect { pages ->
                Log.d(TAG, "observePages: ${pages.size} páginas")
                _pages.value = pages
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

    fun removePage(page: DocumentPageEntity) {
        Log.d(TAG, "removePage: idPage ${page.id}")
        viewModelScope.launch(Dispatchers.IO) {
            documentRepository.deletePage(page)
        }
    }

    fun renameDocument(name: String) {
        viewModelScope.launch {
            documentRepository.renameDocument(documentId, name)
            _documentName.value = name
        }
    }

    fun shareAsPdf() {
        val paths = _pages.value.map { it.imagePath }
        if (paths.isEmpty()) return
        Log.d(TAG, "shareAsPdf: ${paths.size} páginas")
        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(paths, _documentName.value.ifBlank { null })
                .onSuccess { uri ->
                    Log.d(TAG, "shareAsPdf: uri=$uri")
                    documentRepository.markAsExported(documentId)
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

    private val _pageVersions = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val pageVersions: StateFlow<Map<Long, Long>> = _pageVersions.asStateFlow()

    fun notifyPageEdited(pageId: Long) {
        _pageVersions.value = _pageVersions.value + (pageId to System.currentTimeMillis())
    }
}
