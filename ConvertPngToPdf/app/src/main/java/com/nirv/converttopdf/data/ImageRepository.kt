package com.nirv.converttopdf.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ImageRepository {

    private val _images = MutableStateFlow<List<Bitmap>>(emptyList())
    val images: StateFlow<List<Bitmap>> = _images.asStateFlow()

    fun addImage(bitmap: Bitmap) {
        _images.value = _images.value + bitmap
    }

    fun removeImage(index: Int) {
        val current = _images.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _images.value = current
        }
    }

    fun getImages(): List<Bitmap> = _images.value

    fun clear() {
        _images.value = emptyList()
    }

    fun replaceImage(index: Int, bitmap: Bitmap) {
        val current = _images.value.toMutableList()
        if (index in current.indices) {
            current[index] = bitmap
            _images.value = current
        }
    }

    fun reorderImages(from: Int, to: Int) {
        val current = _images.value.toMutableList()
        if (from in current.indices && to in current.indices) {
            val item = current.removeAt(from)
            current.add(to, item)
            _images.value = current
        }
    }
}
