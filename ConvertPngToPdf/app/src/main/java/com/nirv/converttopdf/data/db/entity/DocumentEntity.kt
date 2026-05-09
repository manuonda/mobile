package com.nirv.converttopdf.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey



enum class DocumentType {PROJECT, EXPORTED}

/**
 * Documentos projects que son de type:
 * Project o Un documento de exportacion
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 0,  //cantidad de paginas

    @ColumnInfo(defaultValue = "PROJECT")
    val type: DocumentType = DocumentType.PROJECT,
    val pdfPath: String? = null,
    val parentProjectId: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false
)
