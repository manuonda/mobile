package com.nirv.converttopdf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentPageEntity
import kotlinx.coroutines.flow.Flow

data class FirstPageProjection(val documentId: Long, val imagePath: String)

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY createdAt DESC LIMIT 30")
    fun getRecentDocuments(): Flow<List<DocumentEntity>>

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :docId")
    suspend fun updateFavorite(docId: Long, isFavorite: Boolean)

    @Query("SELECT documentId, imagePath FROM document_pages WHERE pageOrder = 0")
    fun getAllFirstPages(): Flow<List<FirstPageProjection>>

    @Insert
    suspend fun insertDocument(doc: DocumentEntity): Long

    @Query("SELECT * FROM document_pages WHERE documentId = :docId ORDER BY pageOrder ASC")
    fun getPagesForDocument(docId: Long): Flow<List<DocumentPageEntity>>

    @Query("SELECT * FROM document_pages WHERE documentId = :docId ORDER BY pageOrder ASC")
    suspend fun getPagesForDocumentOnce(docId: Long): List<DocumentPageEntity>

    @Insert
    suspend fun insertPage(page: DocumentPageEntity)

    @Query("DELETE FROM documents WHERE id = :docId")
    suspend fun deleteDocumentById(docId: Long)

    @Query("DELETE FROM document_pages where id = :pageId")
    suspend fun deletePageById(pageId: Long)

    @Query("UPDATE documents SET name = :name WHERE id = :docId")
    suspend fun renameDocument(docId: Long, name: String)

    @Query("UPDATE documents SET pageCount = :count WHERE id = :docId")
    suspend fun updatePageCount(docId: Long, count: Int)

    @Query("UPDATE document_pages SET pageOrder = :newOrder WHERE id = :pageId")
    suspend fun updatePageOrder(pageId: Long, newOrder: Int)

    @Query("SELECT * FROM documents WHERE id = :docId")
    suspend fun getDocumentById(docId: Long): DocumentEntity?

    @Query("UPDATE documents SET type = :type WHERE id = :docId")
    suspend fun updateType(docId: Long, type: String)

    @Query("SELECT * FROM documents WHERE parentProjectId = :parentId AND type = 'EXPORTED' LIMIT 1")
    suspend fun findExportedByParent(parentId: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE parentProjectId = :parentId AND type = 'EXPORTED' AND name = :name LIMIT 1")
    suspend fun findExportedByParentAndName(parentId: Long, name: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE parentProjectId = :parentId AND type = 'EXPORTED' ORDER BY createdAt DESC LIMIT 1")
    suspend fun findLatestExportedByParent(parentId: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE parentProjectId = :parentId AND type = 'EXPORTED' LIMIT 1")
    fun getExportedByParentFlow(parentId: Long): Flow<DocumentEntity?>

    @Query("UPDATE documents SET name = :name, pdfPath = :pdfPath, pageCount = :pageCount WHERE id = :docId")
    suspend fun updateExportedDoc(docId: Long, name: String, pdfPath: String, pageCount: Int)
}
