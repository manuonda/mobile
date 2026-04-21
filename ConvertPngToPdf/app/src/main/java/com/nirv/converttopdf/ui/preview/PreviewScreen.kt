package com.nirv.converttopdf.ui.preview

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nirv.converttopdf.ui.theme.PlazoMuted
import com.nirv.converttopdf.ui.theme.PlazoOlive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PreviewScreen(
    documentId: Long,
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onSign: () -> Unit,
    viewModel: PreviewViewModel = koinViewModel(
        key        = "preview_$documentId",
        parameters = { parametersOf(documentId) }
    )
) {
    val images       by viewModel.images.collectAsStateWithLifecycle()
    val shareState   by viewModel.shareState.collectAsStateWithLifecycle()
    val documentName by viewModel.documentName.collectAsStateWithLifecycle()
    val isLoading    by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Log.d("PreviewScreen", "Cargando imágenes con ID :$documentId")
    }

    PreviewScreenContent(
        images        = images,
        shareState    = shareState,
        documentTitle = documentName,
        isLoading     = isLoading,
        onBack        = onBack,
        onAddMore     = onAddMore,
        onSign        = onSign,
        onDeleteImage = { viewModel.removeImage(it) },
        onShare       = { viewModel.shareAsPdf() },
        onResetShareState = { viewModel.resetShareState() },
        onTitleChange = { viewModel.renameDocument(it) }
    )
}

@Composable
fun PreviewScreenContent(
    images: List<Bitmap>,
    shareState: ShareState,
    documentTitle: String,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onSign: () -> Unit,
    onDeleteImage: (Int) -> Unit,
    onShare: () -> Unit,
    onResetShareState: () -> Unit,
    onTitleChange: (String) -> Unit
) {
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput      by remember { mutableStateOf("") }

    LaunchedEffect(shareState) {
        if (shareState is ShareState.Ready) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, shareState.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
            onResetShareState()
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title   = { Text("Renombrar", fontWeight = FontWeight.Bold) },
            text    = {
                OutlinedTextField(
                    value         = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine    = true,
                    placeholder   = { Text("Nombre del documento") },
                    trailingIcon  = {
                        if (renameInput.isNotEmpty()) {
                            IconButton(onClick = { renameInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Borrar")
                            }
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTitleChange(renameInput.trim())
                    showRenameDialog = false
                }) { Text("Aceptar") }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                val displayTitle = documentTitle.ifBlank {
                    if (images.isEmpty()) "Previsualizar" else "Imágenes (${images.size})"
                }
                Text(
                    text = displayTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (documentTitle.isNotBlank()) {
                    IconButton(
                        onClick  = { renameInput = documentTitle; showRenameDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Create, "Renombrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    }
                }
                if (images.isNotEmpty()) {
                    IconButton(
                        onClick  = { images.indices.reversed().forEach { onDeleteImage(it) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Eliminar todas",
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        bottomBar = {
            if (images.isNotEmpty()) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick  = onSign,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, PlazoOlive)
                        ) {
                            Icon(Icons.Default.Create, null, tint = PlazoOlive,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Firmar", fontWeight = FontWeight.SemiBold, color = PlazoOlive)
                        }
                        Button(
                            onClick  = onShare,
                            modifier = Modifier.weight(2f).height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled  = shareState !is ShareState.Loading
                        ) {
                            if (shareState is ShareState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Share, null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Compartir PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Cargando imágenes…",
                            style = MaterialTheme.typography.bodyMedium, color = PlazoMuted)
                    }
                }
            }
            images.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No hay imágenes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(6.dp))
                        Text("Toca el botón + para añadir imágenes",
                            style = MaterialTheme.typography.bodyMedium, color = PlazoMuted)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onAddMore,
                            shape   = RoundedCornerShape(12.dp),
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Añadir imágenes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(images) { index, bitmap ->
                        ImageCard(
                            index    = index,
                            bitmap   = bitmap,
                            onDelete = { onDeleteImage(index) }
                        )
                    }
                    item {
                        AddImageCard(onClick = onAddMore)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCard(index: Int, bitmap: Bitmap, onDelete: () -> Unit) {
    androidx.compose.material3.Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = "Imagen ${index + 1}",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
            // Badge número
            Box(
                modifier = Modifier.padding(6.dp).size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            // Botón cerrar
            Box(
                modifier = Modifier.padding(6.dp).size(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Eliminar",
                        tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun AddImageCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Add, null,
                modifier = Modifier.size(32.dp),
                tint = PlazoMuted)
            Text("Añadir imagen",
                style = MaterialTheme.typography.bodySmall,
                color = PlazoMuted)
        }
    }
}
