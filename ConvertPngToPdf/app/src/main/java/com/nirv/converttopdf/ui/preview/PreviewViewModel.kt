package com.nirv.converttopdf.ui.preview

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentPageEntity
import com.nirv.converttopdf.data.db.entity.DocumentType
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
    data class  Saved(val name: String)          : ShareState()
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

    private val _exportedDocument = MutableStateFlow<DocumentEntity?>(null)
    val exportedDocument: StateFlow<DocumentEntity?> = _exportedDocument.asStateFlow()

    init {
        observePages()
        observeDocumentName()
        observeExportedDocument()
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

    private fun observeExportedDocument() {
        viewModelScope.launch {
            // Derivamos de allDocuments para evitar problemas de serialización del enum.
            // Tomamos el más reciente (mayor createdAt) cuando hay varias versiones.
            documentRepository.allDocuments.collect { docs ->
                _exportedDocument.value = docs
                    .filter { it.parentProjectId == documentId && it.type == DocumentType.EXPORTED }
                    .maxByOrNull { it.createdAt }
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
        // Prioridad: nombre del doc exportado configurado → nombre del proyecto
        val name = _exportedDocument.value?.name ?: _documentName.value.ifBlank { null }
        Log.d(TAG, "shareAsPdf: ${paths.size} páginas, name=$name")
        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(paths, name)
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

    // ── Guardar PDF internamente + upsert en BD ───────────────────────────────
    fun savePdfToApp(customName: String = "") {
        val paths = _pages.value.map { it.imagePath }
        if (paths.isEmpty()) return
        val name = customName.ifBlank { _documentName.value.ifBlank { "documento" } }
        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(paths, name)
                .onSuccess { tempUri ->
                    try {
                        val exportDir = File(context.filesDir, "exports/$documentId").also { it.mkdirs() }
                        val safeName  = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        val destFile  = File(exportDir, "$safeName.pdf")
                        context.contentResolver.openInputStream(tempUri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        documentRepository.upsertExportedDocument(
                            name            = name,
                            pdfPath         = destFile.absolutePath,
                            pageCount       = _pages.value.size,
                            parentProjectId = documentId
                        )
                        Log.d(TAG, "savePdfToApp: guardado en ${destFile.absolutePath}")
                        _shareState.value = ShareState.Saved(name)
                    } catch (e: Exception) {
                        Log.e(TAG, "savePdfToApp: error al copiar/guardar", e)
                        _shareState.value = ShareState.Error(e.message ?: "Error al guardar PDF")
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "savePdfToApp: error al generar", e)
                    _shareState.value = ShareState.Error(e.message ?: "Error al generar PDF")
                }
        }
    }

    // ── Exportar PDF a dispositivo (ACTION_CREATE_DOCUMENT) ───────────────────
    fun exportPdfToDevice(customName: String = "") {
        val paths = _pages.value.map { it.imagePath }
        if (paths.isEmpty()) return
        // Prioridad: nombre pasado por parámetro → nombre doc exportado → nombre proyecto
        val name = customName.ifBlank {
            _exportedDocument.value?.name ?: _documentName.value.ifBlank { "documento" }
        }
        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(paths, name)
                .onSuccess { uri ->
                    Log.d(TAG, "exportPdfToDevice: uri=$uri, name=$name")
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
                val exportName = exportState.suggestedName.removeSuffix(".pdf")
                // upsert: actualiza si ya existe un exportado para este proyecto
                documentRepository.upsertExportedDocument(
                    name            = exportName,
                    pdfPath         = destUri.toString(),
                    pageCount       = _pages.value.size,
                    parentProjectId = documentId
                )
                Log.d(TAG, "writePdfToDevice: registro upserted en BD, name=$exportName")
            } catch (e: Exception) {
                Log.e(TAG, "writePdfToDevice: error", e)
            }
            withContext(Dispatchers.Main) { _shareState.value = ShareState.Idle }
        }
    }
}
