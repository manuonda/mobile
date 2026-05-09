package com.nirv.converttopdf.ui.preview

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "PreviewVM"

sealed class ShareState {
    data object Idle    : ShareState()
    data object Loading : ShareState()
    data class  Ready(val uri: Uri)              : ShareState()
    data class  ReadyImages(val uris: List<Uri>) : ShareState()
    data class  ReadyExport(val uri: Uri, val suggestedName: String) : ShareState()
    data class  Error(val msg: String)           : ShareState()
}

class PreviewViewModel(
    private val documentId: Long,
    private val documentRepository: DocumentRepository,
    private val exportToPdfUseCase: ExportToPdfUseCase,
    private val context: Context
) : ViewModel() {

    private val _pages = MutableStateFlow<List<DocumentPageEntity>>(emptyList())
    val pages: StateFlow<List<DocumentPageEntity>> = _pages.asStateFlow()

    private val _documentName = MutableStateFlow("")
    val documentName: StateFlow<String> = _documentName.asStateFlow()

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pageVersions = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val pageVersions: StateFlow<Map<Long, Long>> = _pageVersions.asStateFlow()

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

    fun notifyPageEdited(pageId: Long) {
        _pageVersions.value = _pageVersions.value + (pageId to System.currentTimeMillis())
    }

    fun resetShareState() {
        _shareState.value = ShareState.Idle
    }

    // ── Compartir como PDF ────────────────────────────────────────────────────
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

    // ── Compartir como imágenes ───────────────────────────────────────────────
    fun shareAsImages() {
        val pageList = _pages.value
        if (pageList.isEmpty()) return
        _shareState.value = ShareState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // Copia a cacheDir para que FileProvider pueda acceder (ya configurado)
                pageList.mapIndexedNotNull { index, page ->
                    val src = File(page.imagePath)
                    if (!src.exists()) return@mapIndexedNotNull null
                    val cached = File(context.cacheDir, "share_img_$index.jpg")
                    src.copyTo(cached, overwrite = true)
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", cached)
                }
            }.onSuccess { uris ->
                Log.d(TAG, "shareAsImages: ${uris.size} imágenes")
                _shareState.value =
                    if (uris.isEmpty()) ShareState.Error("No hay imágenes disponibles")
                    else ShareState.ReadyImages(uris)
            }.onFailure { e ->
                Log.e(TAG, "shareAsImages: error", e)
                _shareState.value = ShareState.Error(e.message ?: "Error al preparar imágenes")
            }
        }
    }

    // ── Exportar PDF a dispositivo (ACTION_CREATE_DOCUMENT) ───────────────────
    fun exportPdfToDevice() {
        val paths = _pages.value.map { it.imagePath }
        if (paths.isEmpty()) return
        val name = _documentName.value.ifBlank { "documento" }
        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(paths, name)
                .onSuccess { uri ->
                    Log.d(TAG, "exportPdfToDevice: uri=$uri")
                    _shareState.value = ShareState.ReadyExport(uri, "$name.pdf")
                }
                .onFailure { e ->
                    Log.e(TAG, "exportPdfToDevice: error", e)
                    _shareState.value = ShareState.Error(e.message ?: "Error al exportar PDF")
                }
        }
    }

    // Llamado desde el callback del CreateDocument launcher con la URI elegida por el usuario
    fun writePdfToDevice(destUri: Uri) {
        val exportState = _shareState.value as? ShareState.ReadyExport ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(exportState.uri)?.use { input ->
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "writePdfToDevice: guardado en $destUri")
            } catch (e: Exception) {
                Log.e(TAG, "writePdfToDevice: error", e)
            }
            withContext(Dispatchers.Main) { _shareState.value = ShareState.Idle }
        }
    }
}
