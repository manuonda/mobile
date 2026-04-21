package com.nirv.converttopdf.ui.preview

import android.content.Intent
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.nirv.converttopdf.ui.theme.PlazoMuted
import com.nirv.converttopdf.ui.theme.PlazoOlive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

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
    val pages        by viewModel.pages.collectAsStateWithLifecycle()
    val shareState   by viewModel.shareState.collectAsStateWithLifecycle()
    val documentName by viewModel.documentName.collectAsStateWithLifecycle()
    val isLoading    by viewModel.isLoading.collectAsStateWithLifecycle()

    PreviewScreenContent(
        pages         = pages,
        shareState    = shareState,
        documentTitle = documentName,
        isLoading     = isLoading,
        onBack        = onBack,
        onAddMore     = onAddMore,
        onSign        = onSign,
        onDeletePage  = { viewModel.removePage(it) },
        onShare       = { viewModel.shareAsPdf() },
        onResetShareState = { viewModel.resetShareState() },
        onTitleChange = { viewModel.renameDocument(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreenContent(
    pages: List<String>,
    shareState: ShareState,
    documentTitle: String,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onSign: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onShare: () -> Unit,
    onResetShareState: () -> Unit,
    onTitleChange: (String) -> Unit
) {
    val context         = LocalContext.current
    val scope           = rememberCoroutineScope()
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput      by remember { mutableStateOf("") }
    var showShareSheet   by remember { mutableStateOf(false) }

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

    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState       = shareSheetState,
            containerColor   = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Exportar documento",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    modifier   = Modifier.padding(bottom = 12.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))

                ShareOptionRow(
                    icon  = Icons.Default.PictureAsPdf,
                    label = "Compartir como PDF",
                    tint  = Color(0xFFE53935)
                ) {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false
                        onShare()
                    }
                }

                ShareOptionRow(
                    icon  = Icons.Default.Image,
                    label = "Compartir como imágenes",
                    tint  = MaterialTheme.colorScheme.primary
                ) {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false
                    }
                }

                ShareOptionRow(
                    icon  = Icons.Default.Edit,
                    label = "Firmar documento",
                    tint  = PlazoOlive
                ) {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false
                        onSign()
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                val displayTitle = documentTitle.ifBlank {
                    if (pages.isEmpty()) "Previsualizar" else "Imágenes (${pages.size})"
                }
                Text(
                    text       = displayTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    modifier   = Modifier.weight(1f)
                )
                    if (documentTitle.isNotBlank()) {
                        IconButton(
                            onClick  = { renameInput = documentTitle; showRenameDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Create, "Renombrar",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                    if (pages.isNotEmpty()) {
                        IconButton(
                            onClick  = { pages.indices.reversed().forEach { onDeletePage(it) } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Eliminar todas",
                                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    BottomBarAction(
                        icon    = Icons.Default.AddAPhoto,
                        label   = "Añadir",
                        onClick = onAddMore
                    )
                    BottomBarAction(
                        icon    = Icons.Default.Edit,
                        label   = "Editar",
                        onClick = { renameInput = documentTitle; showRenameDialog = true }
                    )
                    BottomBarAction(
                        icon    = Icons.Default.Share,
                        label   = if (shareState is ShareState.Loading) "…" else "Compartir",
                        enabled = pages.isNotEmpty() && shareState !is ShareState.Loading,
                        onClick = { showShareSheet = true }
                    )
                    BottomBarAction(
                        icon    = Icons.Default.Settings,
                        label   = "Ajustes",
                        onClick = { }
                    )
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Cargando…",
                            style = MaterialTheme.typography.bodyMedium, color = PlazoMuted)
                    }
                }
            }
            pages.isEmpty() -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
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
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground)
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
                    columns               = GridCells.Fixed(2),
                    modifier              = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                    contentPadding        = PaddingValues(top = 10.dp, bottom = 16.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pages) { index, path ->
                        PageCard(
                            index    = index,
                            path     = path,
                            onDelete = { onDeletePage(index) }
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
private fun PageCard(index: Int, path: String, onDelete: () -> Unit) {
    val context = LocalContext.current
    val imageRequest = remember(path) {
        ImageRequest.Builder(context)
            .data(File(path))
            .memoryCacheKey(path)
            .diskCacheKey(path)
            .size(512)
            .crossfade(false)
            .build()
    }
    androidx.compose.material3.Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            AsyncImage(
                model              = imageRequest,
                contentDescription = "Imagen ${index + 1}",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
            Box(
                modifier = Modifier.padding(6.dp).size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
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
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val effectiveTint = when {
        !enabled                  -> PlazoMuted
        tint != Color.Unspecified -> tint
        else                      -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label,
            tint = effectiveTint, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, color = effectiveTint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ShareOptionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
            .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Add, null,
                modifier = Modifier.size(32.dp),
                tint = PlazoMuted)
            Text("Añadir imagen",
                style = MaterialTheme.typography.bodySmall,
                color = PlazoMuted)
        }
    }
}
