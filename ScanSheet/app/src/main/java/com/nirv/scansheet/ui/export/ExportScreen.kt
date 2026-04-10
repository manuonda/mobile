package com.nirv.scansheet.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    onNewScan: () -> Unit,
    viewModel: ExportViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportar Excel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is ExportUiState.Idle -> {
                    Text(
                        "¿Generar el archivo Excel con los datos escaneados?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.generateExcel(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generar Excel")
                    }
                }

                is ExportUiState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Generando archivo...")
                }

                is ExportUiState.Success -> {
                    val uri = (uiState as ExportUiState.Success).fileUri
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, // placeholder visual
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "¡Excel generado!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.shareFile(context, uri) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Compartir / Guardar")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNewScan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nuevo escaneo")
                    }
                }

                is ExportUiState.Error -> {
                    Text(
                        text = (uiState as ExportUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.generateExcel(context) }) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}
