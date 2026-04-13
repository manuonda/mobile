package com.nirv.converttopdf.ui.capture

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import org.koin.androidx.compose.koinViewModel

// ─────────────────────────────────────────────────────────────────────────────
// CaptureScreen
//
// UX rediseñado:
//   • Grid de galería (3 columnas) — imágenes más recientes primero
//   • Selección múltiple con checkmark teal en esquina superior derecha
//   • Icono de cámara en la TopAppBar → lanza el scanner ML Kit
//   • Botón "Añadir (N)" fijo en la parte inferior, habilitado al seleccionar ≥ 1
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onImagesCaptured: () -> Unit,
    viewModel: CaptureViewModel = koinViewModel()
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val galleryUris  by viewModel.galleryUris.collectAsStateWithLifecycle()
    val selectedUris by viewModel.selectedUris.collectAsStateWithLifecycle()
    val context      = LocalContext.current

    // Permiso de lectura de imágenes (API 33+ usa READ_MEDIA_IMAGES, anterior READ_EXTERNAL_STORAGE)
    val readPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(readPermission)

    // Cargar galería cuando el permiso es concedido
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            viewModel.loadGalleryImages(context.contentResolver)
        }
    }

    // Solicitar permiso automáticamente al entrar
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    // Navegar a Preview cuando la selección es confirmada
    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Done) {
            val count = (uiState as CaptureUiState.Done).imageCount
            viewModel.resetState()
            if (count > 0) onImagesCaptured()
        }
    }

    // Scanner ML Kit
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
                        ?.let { viewModel.onBitmapCaptured(it) }
                }
        }
    }

    CaptureScreenContent(
        galleryUris         = galleryUris,
        selectedUris        = selectedUris,
        permissionGranted   = permissionState.status.isGranted,
        permissionRationale = permissionState.status.shouldShowRationale,
        onBack              = onBack,
        onToggleSelect      = { viewModel.toggleSelection(it) },
        onConfirm           = { viewModel.confirmSelection(context.contentResolver) },
        onRequestPermission = { permissionState.launchPermissionRequest() },
        onScanClick         = {
            val activity = context as? Activity ?: return@CaptureScreenContent
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { e ->
                    viewModel.onError("Scanner no disponible: ${e.message}")
                }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CaptureScreenContent — composable puro
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreenContent(
    galleryUris: List<Uri>,
    selectedUris: Set<Uri>,
    permissionGranted: Boolean,
    permissionRationale: Boolean,
    onBack: () -> Unit,
    onToggleSelect: (Uri) -> Unit,
    onConfirm: () -> Unit,
    onRequestPermission: () -> Unit,
    onScanClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar imágenes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón de cámara / scanner en la barra superior
                    IconButton(onClick = onScanClick) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Escanear documento",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Botón de confirmación fijo abajo — visible solo cuando hay selección
            if (selectedUris.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text       = "Añadir ${selectedUris.size} imagen${if (selectedUris.size > 1) "es" else ""}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->

        when {
            // ── Sin permiso ──────────────────────────────────────────────────
            !permissionGranted -> {
                PermissionPrompt(
                    showRationale    = permissionRationale,
                    onRequestPermission = onRequestPermission,
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            // ── Galería vacía ────────────────────────────────────────────────
            galleryUris.isEmpty() -> {
                Box(
                    modifier          = Modifier.fillMaxSize().padding(padding),
                    contentAlignment  = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No se encontraron imágenes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Usa el icono 📷 para escanear un documento",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Grid de galería ──────────────────────────────────────────────
            else -> {
                LazyVerticalGrid(
                    columns        = GridCells.Fixed(3),
                    modifier       = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement   = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(galleryUris) { uri ->
                        GalleryItem(
                            uri        = uri,
                            isSelected = uri in selectedUris,
                            onToggle   = { onToggleSelect(uri) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GalleryItem — celda del grid con thumbnail + indicador de selección
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GalleryItem(
    uri: Uri,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val teal = Color(0xFF00BFA5)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .clickable { onToggle() }
            .then(
                if (isSelected) Modifier.border(2.dp, teal, RoundedCornerShape(2.dp))
                else Modifier
            )
    ) {
        // Thumbnail de la imagen cargado con Coil
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .size(300)   // tamaño de thumbnail — suficiente para el grid
                .build(),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Overlay oscuro cuando está seleccionada
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
            )
        }

        // Checkmark en esquina superior derecha
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(22.dp)
                .align(Alignment.TopEnd)
                .then(
                    if (isSelected) Modifier
                    else Modifier
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .border(1.5.dp, Color.White, CircleShape)
                )
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Seleccionada",
                    tint     = teal,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PermissionPrompt — pantalla cuando el permiso no está concedido
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionPrompt(
    showRationale: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier             = modifier.padding(32.dp),
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint     = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text  = if (showRationale)
                "Para mostrar tus imágenes necesitamos acceso a la galería. Toca para permitir."
            else
                "Se necesita acceso a la galería para seleccionar imágenes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Conceder permiso")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureScreenPreview() {
    MaterialTheme {
        CaptureScreenContent(
            galleryUris = emptyList(),
            selectedUris = emptySet(),
            permissionGranted = true,
            permissionRationale = false,
            onBack = {},
            onToggleSelect = {},
            onConfirm = {},
            onRequestPermission = {},
            onScanClick = {}
        )
    }
}
