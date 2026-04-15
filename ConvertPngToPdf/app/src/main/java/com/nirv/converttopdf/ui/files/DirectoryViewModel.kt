package com.nirv.converttopdf.ui.files

import android.content.Context
import androidx.lifecycle.ViewModel
import com.nirv.converttopdf.data.ReadPdfRepository
import com.nirv.converttopdf.domain.model.PdfFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


/**
 * ViewModel encargado de gestionar la lógica de la lista de archivos PDF.
 * Actúa como mediador entre el repositorio [ReadPdfRepository] y la interfaz de usuario.
 */
class DirectoryViewModel( private val readPdfRepository: ReadPdfRepository): ViewModel() {

    // Estado privado que el ViewModel puede cambiar (Mutable)
    // MutableStateFlow : Creas un vinculo reactivo entre tus datos y tu pantalla
    private val _pdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())

    // El estado public que la UI puede observar (State)
    val pdfFiles: StateFlow<List<PdfFile>> = _pdfFiles.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles(){
        // Obtenemos los archivos del repositorio y actualizamos el "tubo"
        _pdfFiles.value = readPdfRepository.getGeneratePdfs()
    }

}