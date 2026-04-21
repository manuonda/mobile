package com.nirv.converttopdf.ui.capture

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.nirv.converttopdf.ui.theme.PlazoMuted
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onDocumentReady: (documentId: Long) -> Unit,
    autoLaunchScanner: Boolean = false,
    documentId: Long? = null,
    viewModel: CaptureViewModel = koinViewModel()
) {
    val uiState          by viewModel.uiState.collectAsStateWithLifecycle()
    val galleryUris      by viewModel.galleryUris.collectAsStateWithLifecycle()
    val selectedUris     by viewModel.selectedUris.collectAsStateWithLifecycle()
    val context          = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.clearSelection()
        viewModel.resetState()
    }

    val readPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
        android.Manifest.permission.READ_MEDIA_IMAGES
    else
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionState = rememberPermissionState(readPermission)

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            viewModel.loadGalleryImages(context.contentResolver)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Done) {
            val docId = (uiState as CaptureUiState.Done).documentId
            viewModel.resetState()
            onDocumentReady(docId)
        }
    }

    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setScannerMode(SCANNER_MODE_FULL)
                .setResultFormats(RESULT_FORMAT_JPEG)
                .setPageLimit(10)
                .setGalleryImportAllowed(false)
                .build()
        )
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                ?.pages?.forEach { page ->
                    context.contentResolver.openInputStream(page.imageUri)
                        ?.use { BitmapFactory.decodeStream(it) }
                        ?.let { bitmap ->
                            viewModel.onBitmapCaptured(
                                bitmap     = bitmap,
                                documentId = documentId,
                                documentName = null // El ViewModel o el flujo directo asignarán el nombre
                            )
                        }
                }
        }
    }

    val launchScanner: () -> Unit = {
        val activity = context as? Activity
        if (activity != null) {
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { e -> viewModel.onError("Scanner no disponible: ${e.message}") }
        }
    }

    if (autoLaunchScanner) {
        LaunchedEffect(Unit) { launchScanner() }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        CaptureScreenContent(
            galleryUris         = galleryUris,
            selectedUris        = selectedUris,
            isGalleryLoading    = galleryUris.isEmpty() && permissionState.status.isGranted, 
            permissionGranted   = permissionState.status.isGranted,
            permissionRationale = permissionState.status.shouldShowRationale,
            isAddingToExisting  = documentId != null,
            onBack              = onBack,
            onToggleSelect      = { viewModel.toggleSelection(it) },
            onConfirm           = {
                if (documentId != null) {
                    viewModel.addToExistingDocument(context.contentResolver, documentId)
                } else {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    val defaultName = "CamScanner_$timestamp"
                    viewModel.confirmSelection(context.contentResolver, defaultName)
                }
            },
            onRequestPermission = { permissionState.launchPermissionRequest() },
            onScanClick         = launchScanner
        )
    }
}

@Composable
fun CaptureScreenContent(
    galleryUris: List<Uri>,
    selectedUris: Set<Uri>,
    isGalleryLoading: Boolean,
    permissionGranted: Boolean,
    permissionRationale: Boolean,
    isAddingToExisting: Boolean,
    onBack: () -> Unit,
    onToggleSelect: (Uri) -> Unit,
    onConfirm: () -> Unit,
    onRequestPermission: () -> Unit,
    onScanClick: () -> Unit
) {
    Scaffold(
        topBar = {
            val title = if (selectedUris.isEmpty()) "Seleccionar imágenes" 
                        else "${selectedUris.size} seleccionada${if (selectedUris.size > 1) "s" else ""}"
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.Center))
                
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable(onClick = onScanClick)
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, "Escanear",
                        tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(17.dp))
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !permissionGranted -> PermissionPrompt(permissionRationale, onRequestPermission, Modifier
                    .fillMaxSize()
                    .padding(padding))

                isGalleryLoading -> {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }

                galleryUris.isEmpty() -> Box(Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No se encontraron imágenes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Usa el icono de cámara para escanear", style = MaterialTheme.typography.bodyMedium, color = PlazoMuted)
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(galleryUris) { uri ->
                        GalleryItem(uri, uri in selectedUris) { onToggleSelect(uri) }
                    }
                }
            }

            // Floating action bar
            if (selectedUris.isNotEmpty()) {
                val countLabel = if (selectedUris.size == 1) "1 imagen seleccionada"
                                 else "${selectedUris.size} imágenes seleccionadas"
                val actionLabel = if (isAddingToExisting) "AÑADIR" else "PRÓXIMO"
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F0F0))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onConfirm() }
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryItem(uri: Uri, isSelected: Boolean, onToggle: () -> Unit) {
    Box(modifier = Modifier
        .aspectRatio(1f)
        .clip(RoundedCornerShape(4.dp))
        .clickable { onToggle() }) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true).size(300).build(),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
        )
        if (isSelected) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000)))
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(6.dp)
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.White, CircleShape))
        }
    }
}

@Composable
private fun PermissionPrompt(showRationale: Boolean, onRequestPermission: () -> Unit, modifier: Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(64.dp), tint = PlazoMuted)
        Spacer(Modifier.height(24.dp))
        Text(text = if (showRationale) "Necesitamos acceso a tus fotos." else "Permite el acceso a la galería.",
            style = MaterialTheme.typography.bodyLarge, color = PlazoMuted)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) { Text("Conceder permiso") }
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureScreenPreview() {
    MaterialTheme {
        CaptureScreenContent(emptyList(), emptySet(), false, true, false, false, {}, {}, {}, {}, {})
    }
}
