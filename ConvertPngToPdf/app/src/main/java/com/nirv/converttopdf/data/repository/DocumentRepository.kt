package com.nirv.converttopdf.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.nirv.converttopdf.data.db.dao.DocumentDao
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentPageEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(
    private val dao: DocumentDao,
    private val context: Context
) {
    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()

    // ── Crear un documento nuevo con sus páginas ──────────────────────────────
    suspend fun createDocument(name: String, bitmaps: List<Bitmap>): Long {
        val docId = dao.insertDocument(
            DocumentEntity(name = name, pageCount = bitmaps.size)
        )
        val folder = documentFolder(docId)
        folder.mkdirs()
        bitmaps.forEachIndexed { i, bitmap ->
            val file = File(folder, "page_$i.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            dao.insertPage(
                DocumentPageEntity(documentId = docId, imagePath = file.absolutePath, pageOrder = i)
            )
        }
        return docId
    }

    // ── Agregar páginas a un documento existente ──────────────────────────────
    suspend fun addPagesToDocument(docId: Long, bitmaps: List<Bitmap>) {
        val existing = dao.getPagesForDocumentOnce(docId)
        val startOrder = existing.size
        val folder = documentFolder(docId)
        folder.mkdirs()
        bitmaps.forEachIndexed { i, bitmap ->
            val order = startOrder + i
            val file  = File(folder, "page_$order.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            dao.insertPage(
                DocumentPageEntity(documentId = docId, imagePath = file.absolutePath, pageOrder = order)
            )
        }
        dao.updatePageCount(docId, startOrder + bitmaps.size)
    }

    // ── Cargar bitmaps de un documento desde disco ────────────────────────────
    suspend fun getDocumentBitmaps(docId: Long): List<Bitmap> =
        dao.getPagesForDocumentOnce(docId)
            .sortedBy { it.pageOrder }
            .mapNotNull { page ->
                val file = File(page.imagePath)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }

    // ── Eliminar una página (re-ordena las restantes) ─────────────────────────
    suspend fun deletePage(docId: Long, pageOrder: Int) {
        val pages = dao.getPagesForDocumentOnce(docId)
        val target = pages.find { it.pageOrder == pageOrder } ?: return
        File(target.imagePath).delete()
        dao.deletePage(docId, pageOrder)
        // re-numerar páginas posteriores
        pages.filter { it.pageOrder > pageOrder }
            .forEach { dao.updatePageOrder(it.id, it.pageOrder - 1) }
        dao.updatePageCount(docId, (pages.size - 1).coerceAtLeast(0))
    }

    // ── Renombrar documento ───────────────────────────────────────────────────
    suspend fun renameDocument(docId: Long, name: String) {
        dao.renameDocument(docId, name)
    }

    // ── Eliminar documento completo ───────────────────────────────────────────
    suspend fun deleteDocument(docId: Long) {
        documentFolder(docId).deleteRecursively()
        dao.deleteDocumentById(docId)
    }

    suspend fun getDocumentById(docId: Long): DocumentEntity? =
        dao.getDocumentById(docId)

    // ── Ruta de carpeta de un documento ──────────────────────────────────────
    private fun documentFolder(docId: Long) =
        File(context.filesDir, "documents/$docId")
}
