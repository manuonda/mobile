package com.nirv.converttopdf.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.nirv.converttopdf.data.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportToPdfUseCase(
    private val pdfExporter: PdfExporter
) {
    suspend operator fun invoke(imagePaths: List<String>, title: String? = null): Result<Uri> = withContext(Dispatchers.IO) {
        pdfExporter.exportToPdf(imagePaths, title)
    }

    suspend fun fromBitmaps(bitmaps: List<Bitmap>, title: String? = null): Result<Uri> = withContext(Dispatchers.IO) {
        pdfExporter.exportToPdfFromBitmaps(bitmaps, title)
    }
}
