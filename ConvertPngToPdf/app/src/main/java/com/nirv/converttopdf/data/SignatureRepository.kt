package com.nirv.converttopdf.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Guarda las firmas creadas por el usuario en memoria.
// Es un singleton (single en Koin) para que DrawSignatureViewModel y
// SignatureViewModel compartan la misma instancia de forma reactiva.
class SignatureRepository {

    private val _signatures = MutableStateFlow<List<Bitmap>>(emptyList())
    val signatures: StateFlow<List<Bitmap>> = _signatures.asStateFlow()

    fun addSignature(bitmap: Bitmap) {
        _signatures.value = _signatures.value + bitmap
    }

    fun removeSignature(index: Int) {
        val current = _signatures.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _signatures.value = current
        }
    }

    fun getSignatures(): List<Bitmap> = _signatures.value
}
