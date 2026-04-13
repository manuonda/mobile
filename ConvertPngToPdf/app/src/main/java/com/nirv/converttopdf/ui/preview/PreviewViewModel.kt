package com.nirv.converttopdf.ui.preview

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.ImageRepository
import com.nirv.converttopdf.domain.usecase.ExportToPdfUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ShareState {
    data object Idle    : ShareState()
    data object Loading : ShareState()
    data class  Ready(val uri: Uri)    : ShareState()
    data class  Error(val msg: String) : ShareState()
}

class PreviewViewModel(
    private val imageRepository: ImageRepository,
    private val exportToPdfUseCase: ExportToPdfUseCase
) : ViewModel() {

    val images: StateFlow<List<Bitmap>> = imageRepository.images

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    fun removeImage(index: Int) {
        imageRepository.removeImage(index)
    }

    fun clearAll() {
        imageRepository.clear()
    }

    fun shareAsPdf() {
        val images = imageRepository.getImages()
        if (images.isEmpty()) return

        viewModelScope.launch {
            _shareState.value = ShareState.Loading
            exportToPdfUseCase(images)
                .onSuccess { uri -> _shareState.value = ShareState.Ready(uri) }
                .onFailure { e  -> _shareState.value = ShareState.Error(e.message ?: "Error al generar PDF") }
        }
    }

    fun resetShareState() {
        _shareState.value = ShareState.Idle
    }
}
