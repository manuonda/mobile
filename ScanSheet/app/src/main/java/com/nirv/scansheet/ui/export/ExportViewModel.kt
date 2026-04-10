package com.nirv.scansheet.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.scansheet.domain.usecase.ExportToExcelUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Loading : ExportUiState()
    data class Success(val fileUri: Uri) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

class ExportViewModel(
    private val exportToExcel: ExportToExcelUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun generateExcel(context: Context) {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Loading
            runCatching {
                // Dispatchers.IO: escritura de archivo — nunca bloquear el hilo principal
                withContext(Dispatchers.IO) {
                    val file = exportToExcel()
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                }
            }.onSuccess { uri ->
                _uiState.value = ExportUiState.Success(uri)
            }.onFailure { e ->
                _uiState.value = ExportUiState.Error(e.message ?: "Error al generar Excel")
            }
        }
    }

    // Abre el intent de compartir con el archivo .xlsx
    fun shareFile(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Excel"))
    }
}
