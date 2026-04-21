package com.nirv.converttopdf.ui.preview

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                    placeholder   = { Text("Nombre del documento") }
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
                Text("Exportar documento", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                ShareOptionRow(Icons.Default.PictureAsPdf, "Compartir como PDF", Color(0xFFE53935)) {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false
                        onShare()
                    }
                }
                ShareOptionRow(Icons.Default.Edit, "Firmar documento", PlazoOlive) {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false
                        onSign()
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0), // Insets fijos para evitar parpadeos
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text       = if (isLoading) "Preparando..." else documentTitle.ifBlank { "Imágenes (${pages.size})" },
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp,
                            color      = MaterialTheme.colorScheme.onSurface,
                            maxLines   = 1,
                            modifier   = Modifier.weight(1f)
                        )
                        if (!isLoading && pages.isNotEmpty()) {
                            IconButton(onClick = { pages.indices.reversed().forEach { onDeletePage(it) } }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                }
            }
        },
        bottomBar = {
            // Estructura fija de la BottomBar. No depende del estado de carga para no saltar.
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp) // Altura exacta para estabilidad absoluta
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        BottomBarAction(Icons.Default.AddAPhoto, "Añadir", onClick = onAddMore)
                        BottomBarAction(Icons.Default.Edit, "Editar", onClick = { renameInput = documentTitle; showRenameDialog = true })
                        BottomBarAction(
                            Icons.Default.Share, 
                            if (shareState is ShareState.Loading) "…" else "Exportar", 
                            enabled = pages.isNotEmpty() && !isLoading, 
                            onClick = { showShareSheet = true }
                        )
                        BottomBarAction(Icons.Default.Settings, "Ajustes", onClick = { })
                    }
                }
            }
        }
    ) { padding ->
        // Contenedor principal inamovible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                // El cargador se posiciona sin destruir la estructura del layout
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
                }
            } else if (pages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(64.dp), tint = PlazoMuted.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))
                    Text("Proyecto sin imágenes", style = MaterialTheme.typography.titleMedium, color = PlazoMuted)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onAddMore, shape = RoundedCornerShape(12.dp)) {
                        Text("Empezar a añadir")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    modifier              = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding        = PaddingValues(top = 16.dp, bottom = 24.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(pages) { index, path ->
                        PageCard(index = index, path = path, onDelete = { onDeletePage(index) })
                    }
                    item { AddImageCard(onClick = onAddMore) }
                }
            }
        }
    }
}

@Composable
private fun PageCard(index: Int, path: String, onDelete: () -> Unit) {
    val context = LocalContext.current
    val imageRequest = remember(path) {
        ImageRequest.Builder(context).data(File(path)).size(600).crossfade(true).build()
    }
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = imageRequest, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            
            Box(
                modifier = Modifier.padding(8.dp).size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape).align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            
            IconButton(
                onClick = onDelete, 
                modifier = Modifier.padding(6.dp).size(28.dp).background(Color.Black.copy(0.4f), CircleShape).align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun BottomBarAction(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else PlazoMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ShareOptionRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AddImageCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable { onClick() }
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp), tint = PlazoMuted)
            Text("Añadir", style = MaterialTheme.typography.bodySmall, color = PlazoMuted)
        }
    }
}
