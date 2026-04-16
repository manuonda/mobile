package com.nirv.converttopdf.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_pages",
    foreignKeys = [
        ForeignKey(
            entity      = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns  = ["documentId"],
            onDelete    = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class DocumentPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val imagePath: String,
    val pageOrder: Int
)
