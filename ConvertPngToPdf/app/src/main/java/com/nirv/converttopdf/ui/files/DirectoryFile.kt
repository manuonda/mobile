package com.nirv.converttopdf.ui.files

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryPdf(
    viewModel: DirectoryViewModel = koinViewModel()
){
    // Observamos el stateFlow del ViewModel
    val pdfFiles by viewModel.pdfFiles.collectAsState()
    
}