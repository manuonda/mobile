package com.nirv.converttopdf.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.db.dao.DocumentDao
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilesViewModel(
    private val repo: DocumentRepository,
    private val dao: DocumentDao
) : ViewModel() {

    val allDocs = dao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentDocs = dao.getRecentDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteDocs = dao.getFavoriteDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val firstPages = dao.getAllFirstPages()
        .map { list -> list.associate { it.documentId to it.imagePath } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun toggleFavorite(doc: DocumentEntity) = viewModelScope.launch {
        dao.updateFavorite(doc.id, !doc.isFavorite)
    }

    fun deleteDocument(doc: DocumentEntity) = viewModelScope.launch {
        repo.deleteDocument(doc.id)
    }
}
