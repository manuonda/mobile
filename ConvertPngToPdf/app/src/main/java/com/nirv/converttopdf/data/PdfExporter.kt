package com.nirv.converttopdf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class PdfExporter(private val context: Context) {

    fun exportToPdf(imagePaths: List<String>, title: String? = null): Result<Uri> {
        return try {
            val document = PdfDocument()
            imagePaths.forEachIndexed { index, path ->
                val bitmap = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
                addPage(document, bitmap, index)
                bitmap.recycle()
            }
            writeAndShare(document, title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportToPdfFromBitmaps(bitmaps: List<Bitmap>, title: String? = null): Result<Uri> {
        return try {
            val document = PdfDocument()
            bitmaps.forEachIndexed { index, bitmap -> addPage(document, bitmap, index) }
            writeAndShare(document, title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addPage(document: PdfDocument, bitmap: Bitmap, index: Int) {
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)
    }

    private fun writeAndShare(document: PdfDocument, title: String?): Result<Uri> {
        val safeName = title?.takeIf { it.isNotBlank() }
            ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            ?: "CamScanner_${System.currentTimeMillis()}"
        val file = File(context.cacheDir, "$safeName.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return Result.success(uri)
    }
}
