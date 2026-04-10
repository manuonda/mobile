package com.nirv.converttopdf.ui.preview

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.nirv.converttopdf.data.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreviewViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {

    val images: StateFlow<List<Bitmap>> = imageRepository.images

    fun removeImage(index: Int) {
        imageRepository.removeImage(index)
    }

    fun clearAll() {
        imageRepository.clear()
    }
}
