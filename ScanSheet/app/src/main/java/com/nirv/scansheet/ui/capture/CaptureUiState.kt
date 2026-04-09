package com.nirv.scansheet.ui.capture

sealed class CaptureUiState {
    data object Idle: CaptureUiState()
    data object Loading: CaptureUiState()
    data class Success(
        val rawText: String  // texto crudo reconocido por ML Kit
    ): CaptureUiState()
    data class Error(
        val message: String
    ): CaptureUiState()
    data object Done: CaptureUiState()  //OCR listo -> dispara navegacion a Preview
}