package com.nirv.converttopdf.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DocumentStatus { DRAFT, EXPORTED }

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 0,
    @ColumnInfo(defaultValue = "DRAFT")
    val status: DocumentStatus = DocumentStatus.DRAFT
)
