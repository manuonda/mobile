package com.nirv.converttopdf.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.nirv.converttopdf.data.db.dao.DocumentDao
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentPageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "DocumentRepo"

class DocumentRepository(
    private val dao: DocumentDao,
    private val context: Context
) {
    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()

    // ── Crear un documento nuevo con sus páginas ──────────────────────────────
    suspend fun createDocument(name: String, bitmaps: List<Bitmap>): Long {
        Log.d(TAG, "createDocument: nombre='$name', páginas=${bitmaps.size}")
        val docId = dao.insertDocument(
            DocumentEntity(name = name, pageCount = bitmaps.size)
        )
        val folder = documentFolder(docId)
        folder.mkdirs()
        Log.d(TAG, "createDocument: carpeta=${folder.absolutePath}")
        bitmaps.forEachIndexed { i, bitmap ->
            val file = File(folder, "page_$i.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            Log.d(TAG, "  page_$i.jpg guardado (${file.length()} bytes)")
            dao.insertPage(
                DocumentPageEntity(documentId = docId, imagePath = file.absolutePath, pageOrder = i)
            )
        }
        Log.d(TAG, "createDocument: completado, docId=$docId")
        return docId
    }

    // ── Agregar páginas a un documento existente ──────────────────────────────
    suspend fun addPagesToDocument(docId: Long, bitmaps: List<Bitmap>) {
        val existing = dao.getPagesForDocumentOnce(docId)
        val startOrder = existing.size
        Log.d(TAG, "addPagesToDocument: docId=$docId, páginas existentes=$startOrder, nuevas=${bitmaps.size}")
        val folder = documentFolder(docId)
        folder.mkdirs()
        bitmaps.forEachIndexed { i, bitmap ->
            val order = startOrder + i
            val file  = File(folder, "page_$order.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            Log.d(TAG, "  page_$order.jpg guardado (${file.length()} bytes)")
            dao.insertPage(
                DocumentPageEntity(documentId = docId, imagePath = file.absolutePath, pageOrder = order)
            )
        }
        dao.updatePageCount(docId, startOrder + bitmaps.size)
        Log.d(TAG, "addPagesToDocument: total páginas=${startOrder + bitmaps.size}")
    }

    // ── Flow reactivo de rutas (se actualiza automáticamente con Room) ───────
    fun getDocumentPathsFlow(docId: Long): Flow<List<String>> =
        dao.getPagesForDocument(docId).map { pages ->
            pages.sortedBy { it.pageOrder }
                 .map { it.imagePath }
                 .filter { File(it).exists() }
        }

    // ── Cargar rutas de páginas una vez (para usos puntuales) ────────────────
    suspend fun getDocumentPaths(docId: Long): List<String> =
        withContext(Dispatchers.IO) {
            dao.getPagesForDocumentOnce(docId)
                .sortedBy { it.pageOrder }
                .map { it.imagePath }
                .filter { File(it).exists() }
        }

    // ── Cargar bitmaps full-res (solo para exportar PDF u operaciones) ────────
    suspend fun getDocumentBitmaps(docId: Long): List<Bitmap> =
        withContext(Dispatchers.IO) {
            val pages = dao.getPagesForDocumentOnce(docId).sortedBy { it.pageOrder }
            Log.d(TAG, "getDocumentBitmaps: docId=$docId, páginas en DB=${pages.size}")
            pages.mapNotNull { page ->
                val file = File(page.imagePath)
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    Log.d(TAG, "  order=${page.pageOrder} path=${page.imagePath} → ${if (bmp != null) "${bmp.width}x${bmp.height}" else "NULL (decodeFile falló)"}")
                    bmp
                } else {
                    Log.w(TAG, "  order=${page.pageOrder} ARCHIVO NO EXISTE: ${page.imagePath}")
                    null
                }
            }.also { Log.d(TAG, "getDocumentBitmaps: ${it.size}/${pages.size} bitmaps cargados") }
        }

    // ── Eliminar una página (re-ordena las restantes) ─────────────────────────
    suspend fun deletePage(docId: Long, pageOrder: Int) {
        Log.d(TAG, "deletePage: docId=$docId, pageOrder=$pageOrder")
        val pages = dao.getPagesForDocumentOnce(docId)
        val target = pages.find { it.pageOrder == pageOrder } ?: run {
            Log.w(TAG, "deletePage: no se encontró página con order=$pageOrder")
            return
        }
        File(target.imagePath).delete()
        dao.deletePage(docId, pageOrder)
        pages.filter { it.pageOrder > pageOrder }
            .forEach { dao.updatePageOrder(it.id, it.pageOrder - 1) }
        dao.updatePageCount(docId, (pages.size - 1).coerceAtLeast(0))
        Log.d(TAG, "deletePage: completado, páginas restantes=${(pages.size - 1).coerceAtLeast(0)}")
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
