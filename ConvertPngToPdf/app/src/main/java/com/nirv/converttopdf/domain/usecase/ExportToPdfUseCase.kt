package com.nirv.converttopdf.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.nirv.converttopdf.data.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportToPdfUseCase(
    private val pdfExporter: PdfExporter
) {
    suspend operator fun invoke(images: List<Bitmap>, title: String? = null): Result<Uri> = withContext(Dispatchers.IO) {
        pdfExporter.exportToPdf(images, title)
    }
}
