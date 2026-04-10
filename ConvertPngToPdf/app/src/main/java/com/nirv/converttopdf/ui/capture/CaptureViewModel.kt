package com.nirv.converttopdf.ui.capture

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.nirv.converttopdf.data.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CaptureUiState {
    data object Idle : CaptureUiState()
    data object Loading : CaptureUiState()
    data class Done(val imageCount: Int) : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}

class CaptureViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun onBitmapCaptured(bitmap: Bitmap) {
        imageRepository.addImage(bitmap)
        val count = imageRepository.getImages().size
        _uiState.value = CaptureUiState.Done(count)
    }

    fun onError(message: String) {
        _uiState.value = CaptureUiState.Error(message)
    }

    fun resetState() {
        _uiState.value = CaptureUiState.Idle
    }
}
