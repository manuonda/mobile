package com.nirv.converttopdf.ui.signature

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.nirv.converttopdf.data.SignatureRepository

// ViewModel exclusivo de DrawSignatureScreen.
// Su única responsabilidad: guardar la firma dibujada en el repositorio compartido.
// SignatureViewModel la verá aparecer reactivamente vía SignatureRepository.signatures.
class DrawSignatureViewModel(
    private val signatureRepository: SignatureRepository
) : ViewModel() {

    fun saveSignature(bitmap: Bitmap) {
        signatureRepository.addSignature(bitmap)
    }
}
