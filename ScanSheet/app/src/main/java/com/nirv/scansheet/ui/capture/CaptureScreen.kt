package com.nirv.scansheet.ui.capture

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onTextRecognized: () -> Unit,
    viewModel: CaptureViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity

    // Navega a Preview cuando el OCR termina
    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Done) {
            viewModel.resetState()
            onTextRecognized()
        }
    }

    // Recibe el resultado del Document Scanner (imagen ya recortada y corregida)
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val imageUri = scanResult?.pages?.firstOrNull()?.imageUri
            if (imageUri != null) {
                // Convierte la URI de la imagen escaneada a Bitmap y lanza el OCR
                val bitmap = context.contentResolver
                    .openInputStream(imageUri)
                    ?.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    viewModel.onBitmapCaptured(bitmap)
                } else {
                    viewModel.onError("No se pudo leer la imagen escaneada")
                }
            }
        }
    }

    // Configuración del Document Scanner:
    // SCANNER_MODE_FULL → incluye detección de bordes, corrección de perspectiva y mejora de imagen
    // remember: el scanner se crea una sola vez, no se recrea en cada recomposición
    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setScannerMode(SCANNER_MODE_FULL)
                .setResultFormats(RESULT_FORMAT_JPEG)
                .setPageLimit(1)
                .setGalleryImportAllowed(true)
                .build()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear Documento") },
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
                is CaptureUiState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Procesando OCR...")
                    }
                }

                is CaptureUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = (uiState as CaptureUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Reintentar")
                        }
                    }
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Escaneá el documento",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "El scanner detectará los bordes y corregirá la perspectiva automáticamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = {
                                // Lanza la Activity del Document Scanner
                                scanner.getStartScanIntent(activity)
                                    .addOnSuccessListener { intentSender ->
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        viewModel.onError("Scanner no disponible: ${e.message}")
                                    }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Abrir Scanner")
                        }
                    }
                }
            }
        }
    }
}
