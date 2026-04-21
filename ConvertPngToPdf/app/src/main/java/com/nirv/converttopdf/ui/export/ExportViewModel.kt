package com.nirv.converttopdf.ui.export

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.ImageRepository
import com.nirv.converttopdf.domain.usecase.ExportToPdfUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Loading : ExportUiState()
    data class Success(val fileUri: Uri) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

class ExportViewModel(
    application: Application,
    private val imageRepository: ImageRepository,
    private val exportToPdfUseCase: ExportToPdfUseCase
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>()

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun generatePdf() {
        val images = imageRepository.getImages()
        if (images.isEmpty()) {
            _uiState.value = ExportUiState.Error("No hay imágenes para exportar")
            return
        }

        viewModelScope.launch {
            _uiState.value = ExportUiState.Loading
            exportToPdfUseCase.fromBitmaps(images)
                .onSuccess { uri ->
                    _uiState.value = ExportUiState.Success(uri)
                }
                .onFailure { error ->
                    _uiState.value = ExportUiState.Error(error.message ?: "Error desconocido")
                }
        }
    }

    fun shareFile(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
    }

    fun resetState() {
        _uiState.value = ExportUiState.Idle
    }
}
