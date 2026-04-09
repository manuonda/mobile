package com.nirv.scansheet.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onTextRecognized: () -> Unit,
    viewModel: CaptureViewModel = koinViewModel()
) {
    // Se cambió 'var' por 'val' y ahora requiere el import de 'getValue'
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Efecto para navegar cuando el estado cambie a Done
    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Done) {
            viewModel.resetState() // Limpiamos el estado para que no dispare la navegación infinitamente
            onTextRecognized()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanea el Documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is CaptureUiState.Loading -> CircularProgressIndicator()
                is CaptureUiState.Error -> Text(
                    text = (uiState as CaptureUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                // Incluye Idle, Success y Done (mientras navega)
                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Cámara — placeholder", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(24.dp))
                    // Simula que el OCR procesó algo
                    Button(onClick = { viewModel.onTextRecognized("texto simulado") }) {
                        Text("Simular escaneo")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureScreenPreview() {
    CaptureScreen(onBack = {}, onTextRecognized = {})
}
