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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onImagesCaptured: () -> Unit,
    autoLaunchScanner: Boolean = false,
    viewModel: CaptureViewModel = koinViewModel()
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val galleryUris  by viewModel.galleryUris.collectAsStateWithLifecycle()
    val selectedUris by viewModel.selectedUris.collectAsStateWithLifecycle()
    val context      = LocalContext.current

    val readPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(readPermission)

    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            viewModel.loadGalleryImages(context.contentResolver)
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Done) {
            val count = (uiState as CaptureUiState.Done).imageCount
            viewModel.resetState()
            if (count > 0) onImagesCaptured()
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
                        ?.let { viewModel.onBitmapCaptured(it) }
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
                .addOnFailureListener { e ->
                    viewModel.onError("Scanner no disponible: ${e.message}")
                }
        }
    }

    if (autoLaunchScanner) {
        LaunchedEffect(Unit) { launchScanner() }
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        CaptureScreenContent(
            galleryUris         = galleryUris,
            selectedUris        = selectedUris,
            permissionGranted   = permissionState.status.isGranted,
            permissionRationale = permissionState.status.shouldShowRationale,
            onBack              = onBack,
            onToggleSelect      = { viewModel.toggleSelection(it) },
            onConfirm           = { viewModel.confirmSelection(context.contentResolver) },
            onRequestPermission = { permissionState.launchPermissionRequest() },
            onScanClick         = launchScanner
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CaptureScreenContent — composable puro (Plazo style)
// ─────────────────────────────────────────────────────────────────────────────

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
            // ── TopBar compacto con título centrado (Plazo style) ─────────────
            val title = if (selectedUris.isEmpty()) "Seleccionar imágenes"
                        else "${selectedUris.size} seleccionada${if (selectedUris.size > 1) "s" else ""}"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Izquierda: volver
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier.size(40.dp).align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint               = MaterialTheme.colorScheme.onSurface,
                        modifier           = Modifier.size(18.dp)
                    )
                }
                // Centro: título
                Text(
                    text       = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.align(Alignment.Center)
                )
                // Derecha: botón cámara en círculo lila
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
                    Icon(
                        imageVector        = Icons.Default.CameraAlt,
                        contentDescription = "Escanear",
                        tint               = MaterialTheme.colorScheme.onBackground,
                        modifier           = Modifier.size(17.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (selectedUris.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)   // Plazo: rounded rect, no pill
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
            !permissionGranted -> {
                PermissionPrompt(
                    showRationale       = permissionRationale,
                    onRequestPermission = onRequestPermission,
                    modifier            = Modifier.fillMaxSize().padding(padding)
                )
            }

            galleryUris.isEmpty() -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No se encontraron imágenes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Usa el icono de cámara para escanear un documento",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns        = GridCells.Fixed(3),
                    modifier       = Modifier.fillMaxSize().padding(padding),
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
// GalleryItem — thumbnail + indicador de selección lima
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GalleryItem(
    uri: Uri,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onToggle() }
            .then(
                if (isSelected)
                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                else Modifier
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .size(300)
                .build(),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
            )
        }

        // Indicador de selección — lima cuando seleccionado
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(24.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color(0x55000000)
                )
                .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionada",
                    tint               = MaterialTheme.colorScheme.onPrimary,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PermissionPrompt
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
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint     = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text  = if (showRationale)
                "Para mostrar tus imágenes necesitamos acceso a la galería. Toca para permitir."
            else
                "Se necesita acceso a la galería para seleccionar imágenes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Conceder permiso", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CaptureScreenPreview() {
    MaterialTheme {
        CaptureScreenContent(
            galleryUris         = emptyList(),
            selectedUris        = emptySet(),
            permissionGranted   = true,
            permissionRationale = false,
            onBack              = {},
            onToggleSelect      = {},
            onConfirm           = {},
            onRequestPermission = {},
            onScanClick         = {}
        )
    }
}
