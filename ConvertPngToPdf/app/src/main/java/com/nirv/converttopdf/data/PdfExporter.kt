package com.nirv.converttopdf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class PdfExporter(private val context: Context) {

    fun exportToPdf(images: List<Bitmap>, title: String? = null): Result<Uri> {
        return try {
            val document = PdfDocument()

            images.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = document.startPage(pageInfo)

                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
            }

            val safeName = title?.takeIf { it.isNotBlank() }
                ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                ?: "CamScanner_${System.currentTimeMillis()}"
            val fileName = "$safeName.pdf"
            val file = File(context.cacheDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
