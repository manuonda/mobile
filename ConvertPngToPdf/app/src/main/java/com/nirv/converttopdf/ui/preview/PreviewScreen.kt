package com.nirv.converttopdf.ui.preview

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
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
        pages             = pages,
        shareState        = shareState,
        documentTitle     = documentName,
        isLoading         = isLoading,
        onBack            = onBack,
        onAddMore         = onAddMore,
        onSign            = onSign,
        onDeletePage      = { viewModel.removePage(it) },
        onShare           = { viewModel.shareAsPdf() },
        onResetShareState = { viewModel.resetShareState() },
        onTitleChange     = { viewModel.renameDocument(it) }
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

    // Shimmer compartido — una sola animación para todas las celdas
    val shimmerBrush = rememberShimmerBrush()

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
            title   = { Text("Renombrar proyecto", fontWeight = FontWeight.Bold) },
            text    = {
                OutlinedTextField(
                    value         = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine    = true,
                    placeholder   = { Text("Ej: Factura de luz") },
                    shape         = RoundedCornerShape(12.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(
                    onClick = { onTitleChange(renameInput.trim()); showRenameDialog = false }
                ) { Text("Guardar") }
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
                Text("Exportar documento",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                ShareOptionRow(Icons.Default.PictureAsPdf, "Compartir como PDF", Color(0xFFE53935)) {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false; onShare()
                    }
                }
                ShareOptionRow(Icons.Default.Draw, "Firmar documento", PlazoOlive) {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false; onSign()
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            // navigationBarsPadding en el Column externo → Scaffold mide alto correcto desde frame 1
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                            modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = documentTitle.ifBlank { "Nuevo Proyecto" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (isLoading) "Cargando…" else "${pages.size} páginas",
                            style = MaterialTheme.typography.labelSmall,
                            color = PlazoMuted
                        )
                    }
                    // Siempre reserva el mismo espacio → topBar altura estable
                    IconButton(
                        onClick  = { renameInput = documentTitle; showRenameDialog = true },
                        modifier = Modifier.size(40.dp),
                        enabled  = documentTitle.isNotBlank()
                    ) {
                        Icon(Icons.Default.Edit, "Renombrar",
                            tint = if (documentTitle.isNotBlank()) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            // navigationBarsPadding en Column externo → altura estable desde frame 1
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Botones con enabled fijo para Añadir — siempre activo
                    BottomBarAction(Icons.Default.AddAPhoto, "Añadir", onClick = onAddMore)
                    BottomBarAction(
                        icon    = Icons.Default.PictureAsPdf,
                        label   = if (shareState is ShareState.Loading) "…" else "PDF",
                        enabled = pages.isNotEmpty() && shareState !is ShareState.Loading,
                        tint    = Color(0xFFE53935),
                        onClick = { showShareSheet = true }
                    )
                    BottomBarAction(
                        icon    = Icons.Default.Draw,
                        label   = "Firmar",
                        enabled = pages.isNotEmpty(),
                        tint    = PlazoOlive,
                        onClick = onSign
                    )
                    BottomBarAction(
                        icon    = Icons.Default.Share,
                        label   = "Enviar",
                        enabled = pages.isNotEmpty(),
                        onClick = { showShareSheet = true }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                pages.isEmpty() && !isLoading -> EmptyPreviewState(onAddMore)
                else -> {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(2),
                        modifier              = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding        = PaddingValues(top = 12.dp, bottom = 20.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mientras isLoading muestra 6 celdas shimmer con misma estructura
                        if (isLoading) {
                            items(6) {
                                ShimmerCard(brush = shimmerBrush)
                            }
                        } else {
                            itemsIndexed(pages) { index, path ->
                                PageCard(
                                    index        = index,
                                    path         = path,
                                    shimmerBrush = shimmerBrush,
                                    onDelete     = { onDeletePage(index) }
                                )
                            }
                            item { AddImageCard(onClick = onAddMore) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Shimmer compartido (una sola InfiniteTransition para toda la pantalla) ──

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant,
        ),
        start = Offset(offset - 300f, offset - 300f),
        end   = Offset(offset, offset)
    )
}

// ─── Celda shimmer (estructura idéntica a PageCard para evitar salto visual) ─

@Composable
private fun ShimmerCard(brush: Brush) {
    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth().aspectRatio(0.75f)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush))
    }
}

// ─── Celda real: SubcomposeAsyncImage muestra shimmer mientras carga ─────────

@Composable
private fun PageCard(index: Int, path: String, shimmerBrush: Brush, onDelete: () -> Unit) {
    val context = LocalContext.current
    val request = remember(path) {
        ImageRequest.Builder(context)
            .data(File(path))
            .memoryCacheKey(path)
            .diskCacheKey(path)
            .size(600)
            .build()
    }
    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth().aspectRatio(0.75f)
    ) {
        Box {
            SubcomposeAsyncImage(
                model              = request,
                contentDescription = "Imagen ${index + 1}",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.fillMaxSize().background(shimmerBrush))
                }
            )
            // Badge número
            Surface(
                color    = MaterialTheme.colorScheme.primary,
                shape    = CircleShape,
                modifier = Modifier.padding(8.dp).size(24.dp).align(Alignment.TopStart)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${index + 1}", fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            // Botón eliminar
            IconButton(
                onClick  = onDelete,
                modifier = Modifier
                    .padding(6.dp).size(28.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, "Eliminar",
                    tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─── BottomBarAction ─────────────────────────────────────────────────────────

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val color = when {
        !enabled                  -> PlazoMuted
        tint != Color.Unspecified -> tint
        else                      -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .width(56.dp)
    ) {
        Icon(icon, contentDescription = label,
            tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

// ─── ShareOptionRow ───────────────────────────────────────────────────────────

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

// ─── Estado vacío ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyPreviewState(onAddMore: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.FilterFrames, null,
            modifier = Modifier.size(64.dp),
            tint = PlazoMuted.copy(alpha = 0.2f))
        Spacer(Modifier.height(16.dp))
        Text("No hay imágenes",
            style = MaterialTheme.typography.titleMedium,
            color = PlazoMuted)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddMore, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Añadir imágenes")
        }
    }
}

// ─── Añadir imagen card ───────────────────────────────────────────────────────

@Composable
private fun AddImageCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Add, null,
                modifier = Modifier.size(32.dp), tint = PlazoMuted)
            Text("Añadir", style = MaterialTheme.typography.bodySmall, color = PlazoMuted)
        }
    }
}
