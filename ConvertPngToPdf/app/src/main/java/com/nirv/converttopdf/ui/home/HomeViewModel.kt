package com.nirv.converttopdf.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nirv.converttopdf.data.ReadPdfRepository
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.repository.DocumentRepository
import com.nirv.converttopdf.domain.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val documentRepository: DocumentRepository,
    private val readPdfRepository: ReadPdfRepository
) : ViewModel() {

    private val _pdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())
    val pdfFiles: StateFlow<List<PdfFile>> = _pdfFiles.asStateFlow()

    private val _draftDocuments = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val draftDocuments: StateFlow<List<DocumentEntity>> = _draftDocuments.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            // Cargar borradores desde la base de datos
            documentRepository.allDocuments.collect { documents ->
                _draftDocuments.value = documents
            }
        }
        refreshPdfFiles()
    }

    fun refreshPdfFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _pdfFiles.value = readPdfRepository.getGeneratePdfs()
        }
    }
}
