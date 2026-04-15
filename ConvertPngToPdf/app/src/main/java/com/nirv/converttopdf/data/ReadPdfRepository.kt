package com.nirv.converttopdf.data

import android.R
import android.content.Context
import androidx.core.content.FileProvider
import com.nirv.converttopdf.domain.model.PdfFile
import java.io.File

class ReadPdfRepository (private val context: Context){

    //Definimos la carpeta donde vieven lso pdfs
    private val pdfFolder = File(context.filesDir, "pdfs").apply {
        if(!exists()) mkdirs()
    }

    fun getGeneratePdfs(): List<PdfFile> {
       //Listamos los archivos .pdf y los mapeamos a nuestro
       // modelo y ordenamos  por fecha y modificacion
       return pdfFolder.listFiles { file -> file.extension == "pdf"}
           ?.map {
               file ->
               PdfFile(
                   name = file.name,
                   path = file.absolutePath,
                   size = formatFileSize(file.length()),
                   lastModified = file.lastModified(),
                   uri = FileProvider.getUriForFile(
                       context,
                       "${context.packageName}.provider",
                       file
                   )
               )
           }
           ?.sortedByDescending { it.lastModified }
           ?: emptyList()
    }


    fun deleteFile(path: String) : Boolean{
        val file = File(path)
        return if( file.exists()) file.delete() else false
    }

    private fun formatFileSize(size:Long) : String {
        if(size<= 0 ) return "0 B"
        val units = arrayOf("B","KB","MB","GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)) .toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

}