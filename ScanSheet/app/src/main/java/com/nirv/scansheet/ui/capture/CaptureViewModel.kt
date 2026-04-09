package com.nirv.scansheet.ui.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.scansheet.data.ocr.MlKitOcrProcessor
import com.nirv.scansheet.domain.usecase.ProcessOcrResultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de captura.
 * Koin inyecta las dependencias declaradas en AppModule (ocrProcessor, processOcrResult).
 * La misma instancia de ScanRepository llega aquí y a PreviewViewModel via Koin.
 */
class CaptureViewModel(
    private val ocrProcessor: MlKitOcrProcessor,
    private val processOcrResult: ProcessOcrResultUseCase
) : ViewModel() {

    // Estado privado mutable: "Fuente de verdad" que solo este ViewModel puede modificar.
    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)

    // Estado público de solo lectura: La UI se suscribe a este flujo para actualizarse.
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    /**
     * Llamar cuando CameraX entrega el Bitmap capturado.
     * Corre el OCR real con ML Kit.
     */
    fun onBitmapCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = CaptureUiState.Loading
            runCatching {
                val rawLines = ocrProcessor.process(bitmap)
                processOcrResult(rawLines)
            }.onSuccess {
                _uiState.value = CaptureUiState.Done
            }.onFailure { error ->
                _uiState.value = CaptureUiState.Error(error.message ?: "Error al procesar imagen")
            }
        }
    }

    /**
     * Simulación para el botón placeholder de la UI.
     * Se elimina cuando CameraX esté integrado.
     */
    fun onTextRecognized(rawText: String) {
        viewModelScope.launch {
            _uiState.value = CaptureUiState.Loading
            processOcrResult(listOf(rawText))
            _uiState.value = CaptureUiState.Done
        }
    }

    /**
     * Maneja errores durante el proceso de captura o procesamiento de OCR.
     */
    fun onError(message: String) {
        _uiState.value = CaptureUiState.Error(message)
    }

    /**
     * Restablece la pantalla al estado inicial (Cámara activa esperando captura).
     */
    fun resetState() {
        _uiState.value = CaptureUiState.Idle
    }
}
