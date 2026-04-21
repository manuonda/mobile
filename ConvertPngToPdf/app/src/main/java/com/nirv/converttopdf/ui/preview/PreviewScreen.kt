package com.nirv.converttopdf.ui.preview

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nirv.converttopdf.ui.theme.PlazoMuted
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

    LaunchedEffect(Unit) {
        Log.d("PreviewScreen", "Cargando imágenes con ID :$documentId")
    }

    PreviewScreenContent(
        images        = images,
        shareState    = shareState,
        documentTitle = documentName,
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

    // ── Diálogo renombrar ──────────────────────────────────────────────────────
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
            val displayTitle = documentTitle.ifBlank {
                if (images.isEmpty()) "Previsualizar" else "Imágenes (${images.size})"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp).align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }
                // Título + lápiz centrados
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayTitle, fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1
                    )
                    if (documentTitle.isNotBlank()) {
                        IconButton(
                            onClick  = { renameInput = documentTitle; showRenameDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Create, "Renombrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                // Icono delete (eliminar todas) — derecha
                if (images.isNotEmpty()) {
                    IconButton(
                        onClick  = { images.indices.reversed().forEach { onDeleteImage(it) } },
                        modifier = Modifier.size(40.dp).align(Alignment.CenterEnd)
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
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        BottomBarAction(
                            icon = Icons.Default.AddAPhoto, label = "Añadir",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = onAddMore, modifier = Modifier.weight(1f)
                        )
                        BottomBarAction(
                            icon = Icons.Default.Share, label = "Compartir",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = onShare, modifier = Modifier.weight(1f),
                            loading = shareState is ShareState.Loading
                        )
                        BottomBarAction(
                            icon = Icons.Default.Create, label = "Firmar",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = onSign, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (images.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddAPhoto, null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No hay imágenes", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(6.dp))
                    Text("Usa Añadir para escanear o seleccionar imágenes",
                        style = MaterialTheme.typography.bodyMedium, color = PlazoMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(6.dp)) }
                itemsIndexed(images) { index, bitmap ->
                    ImageCard(index = index, bitmap = bitmap, onDelete = { onDeleteImage(index) })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BottomBarAction — icono + etiqueta estilo CamScanner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        IconButton(onClick = onClick, enabled = !loading, modifier = Modifier.size(48.dp)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp, color = tint)
            } else {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
            }
        }
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = tint, maxLines = 1)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ImageCard — thumbnail con badge numérico + botón eliminar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImageCard(index: Int, bitmap: Bitmap, onDelete: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(), contentDescription = "Imagen ${index + 1}",
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        // Badge número
        Box(
            modifier = Modifier.padding(8.dp).size(26.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        // Botón cerrar
        Box(
            modifier = Modifier.padding(6.dp).size(28.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Eliminar", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
