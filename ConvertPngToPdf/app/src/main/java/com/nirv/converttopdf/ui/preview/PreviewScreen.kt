package com.nirv.converttopdf.ui.preview

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.nirv.converttopdf.data.db.entity.DocumentPageEntity
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
    onEditPage: (pageId: Long, imagePath: String) -> Unit = { _, _ -> },
    viewModel: PreviewViewModel = koinViewModel(
        key        = "preview_$documentId",
        parameters = { parametersOf(documentId) }
    )
) {
    val pages        by viewModel.pages.collectAsStateWithLifecycle()
    val shareState   by viewModel.shareState.collectAsStateWithLifecycle()
    val documentName by viewModel.documentName.collectAsStateWithLifecycle()
    val isLoading    by viewModel.isLoading.collectAsStateWithLifecycle()
    val pageVersions by viewModel.pageVersions.collectAsStateWithLifecycle()

    PreviewScreenContent(
        pages              = pages,
        shareState         = shareState,
        documentTitle      = documentName,
        isLoading          = isLoading,
        pageVersions       = pageVersions,
        onBack             = onBack,
        onAddMore          = onAddMore,
        onSign             = onSign,
        onEditPage         = onEditPage,
        onDeletePage       = { page -> viewModel.removePage(page) },
        onShare            = { viewModel.shareAsPdf() },
        onShareImages      = { viewModel.shareAsImages() },
        onExportPdf        = { viewModel.exportPdfToDevice() },
        onWritePdfToDevice = { uri -> viewModel.writePdfToDevice(uri) },
        onResetShareState  = { viewModel.resetShareState() },
        onTitleChange      = { viewModel.renameDocument(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreenContent(
    pages: List<DocumentPageEntity>,
    shareState: ShareState,
    documentTitle: String,
    isLoading: Boolean = false,
    pageVersions: Map<Long, Long> = emptyMap(),
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onSign: () -> Unit,
    onEditPage: (pageId: Long, imagePath: String) -> Unit = { _, _ -> },
    onDeletePage: (DocumentPageEntity) -> Unit,
    onShare: () -> Unit,
    onShareImages: () -> Unit,
    onExportPdf: () -> Unit,
    onWritePdfToDevice: (Uri) -> Unit,
    onResetShareState: () -> Unit,
    onTitleChange: (String) -> Unit
) {
    val context         = LocalContext.current
    val scope           = rememberCoroutineScope()
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput      by remember { mutableStateOf("") }
    var showShareSheet   by remember { mutableStateOf(false) }
    var showDocSettings  by remember { mutableStateOf(false) }

    // PDF export settings (local — se aplican al compartir)
    var pdfExportName  by remember(documentTitle) { mutableStateOf(documentTitle) }
    var pdfGrayscale   by remember { mutableStateOf(false) }
    var pdfWhiteMargin by remember { mutableStateOf(false) }
    var pdfQuality     by remember { mutableStateOf("Alto") }

    val shimmerBrush = rememberShimmerBrush()

    // Launcher para "Exportar páginas como PDF" — el usuario elige dónde guardar
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) onWritePdfToDevice(uri)
        else onResetShareState()
    }

    LaunchedEffect(shareState) {
        when (shareState) {
            is ShareState.Ready -> {
                val uri = shareState.uri
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type     = "application/pdf"
                    clipData = ClipData.newRawUri("PDF", uri)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Compartir PDF").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
                onResetShareState()
            }
            is ShareState.ReadyImages -> {
                val uris = ArrayList(shareState.uris)
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type     = "image/jpeg"
                    clipData = ClipData.newRawUri("", uris.first()).also { clip ->
                        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
                    }
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Compartir imágenes").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
                onResetShareState()
            }
            is ShareState.ReadyExport -> {
                exportLauncher.launch(shareState.suggestedName)
                // El estado se resetea en writePdfToDevice o si el usuario cancela
            }
            else -> Unit
        }
    }

    // ── Diálogo de renombrar ──────────────────────────────────────────────────
    if (showRenameDialog) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title   = { Text("Renombrar proyecto", fontWeight = FontWeight.Bold) },
            text    = {
                OutlinedTextField(
                    value         = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine    = true,
                    label         = { Text("Nombre del proyecto") },
                    shape         = RoundedCornerShape(12.dp),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    trailingIcon  = {
                        if (renameInput.isNotEmpty()) {
                            IconButton(onClick = { renameInput = "" }) {
                                Icon(
                                    Icons.Default.Close, "Borrar texto",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
                        focusedContainerColor   = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        cursorColor             = MaterialTheme.colorScheme.primary
                    )
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRenameDialog = false },
                    shape   = RoundedCornerShape(50)
                ) { Text("Cancelar") }
            },
            confirmButton = {
                Button(
                    onClick  = { onTitleChange(renameInput.trim()); showRenameDialog = false },
                    enabled  = renameInput.trim().isNotBlank(),
                    shape    = RoundedCornerShape(50)
                ) { Text("Guardar") }
            }
        )
    }

    // ── Share bottom sheet ────────────────────────────────────────────────────
    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState       = shareSheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ShareBottomSheetContent(
                isLoading     = shareState is ShareState.Loading,
                onSharePdf    = {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false; onShare()
                    }
                },
                onShareImages = {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false; onShareImages()
                    }
                },
                onExportPdf   = {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false; onExportPdf()
                    }
                },
                onSign        = {
                    scope.launch { shareSheetState.hide() }.invokeOnCompletion {
                        showShareSheet = false; onSign()
                    }
                }
            )
        }
    }

    // ── Configuración del documento ───────────────────────────────────────────
    if (showDocSettings) {
        ModalBottomSheet(
            onDismissRequest = { showDocSettings = false },
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                // ── Título + cerrar ───────────────────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Configurar PDF",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showDocSettings = false }) {
                        Icon(Icons.Default.Close, "Cerrar")
                    }
                }

                // ── Nombre del archivo ────────────────────────────────────────
                OutlinedTextField(
                    value         = pdfExportName,
                    onValueChange = { pdfExportName = it },
                    label         = { Text("Nombre del archivo PDF", fontSize = 12.sp) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    modifier      = Modifier.fillMaxWidth(),
                    suffix        = { Text(".pdf", color = PlazoMuted, fontSize = 13.sp) },
                    trailingIcon  = {
                        if (pdfExportName.isNotEmpty()) {
                            IconButton(onClick = { pdfExportName = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
                        focusedContainerColor   = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        cursorColor             = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 8.dp),
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    thickness = 0.5.dp
                )

                // ── Escala de grises ──────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Aplicar escala de grises", style = MaterialTheme.typography.bodyMedium)
                        Text("Convierte las páginas a blanco y negro", style = MaterialTheme.typography.bodySmall, color = PlazoMuted)
                    }
                    Switch(checked = pdfGrayscale, onCheckedChange = { pdfGrayscale = it })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), thickness = 0.5.dp)

                // ── Margen blanco ─────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Insertar margen blanco", style = MaterialTheme.typography.bodyMedium)
                        Text("Agrega margen alrededor de cada página", style = MaterialTheme.typography.bodySmall, color = PlazoMuted)
                    }
                    Switch(checked = pdfWhiteMargin, onCheckedChange = { pdfWhiteMargin = it })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), thickness = 0.5.dp)

                Spacer(Modifier.height(12.dp))

                // ── Calidad de imagen ─────────────────────────────────────────
                Text(
                    "Calidad de imagen",
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Alto", "Medio", "Bajo").forEach { q ->
                        FilterChip(
                            selected = pdfQuality == q,
                            onClick  = { pdfQuality = q },
                            label    = { Text(q, fontSize = 13.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Botones acción ────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = { showDocSettings = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp)
                    ) { Text("Cancelar") }

                    Button(
                        onClick  = { showDocSettings = false; showShareSheet = true },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        enabled  = pages.isNotEmpty()
                    ) { Text("Compartir PDF") }
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
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = documentTitle.ifBlank { "Nuevo Proyecto" },
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines   = 1
                        )
                        Text(
                            text  = if (isLoading) "Cargando…" else "${pages.size} páginas",
                            style = MaterialTheme.typography.labelSmall,
                            color = PlazoMuted
                        )
                    }
                    IconButton(
                        onClick  = { renameInput = documentTitle; showRenameDialog = true },
                        modifier = Modifier.size(40.dp),
                        enabled  = documentTitle.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Edit, "Renombrar",
                            tint     = if (documentTitle.isNotBlank()) MaterialTheme.colorScheme.onSurface
                                       else Color.Transparent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Añadir — botón secundario compacto
                    Row(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onAddMore() }
                            .padding(horizontal = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "Añadir",
                            modifier = Modifier.size(18.dp),
                            tint     = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Añadir",
                            fontWeight = FontWeight.Medium,
                            fontSize   = 14.sp,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Compartir — botón primario que ocupa el resto del ancho
                    val isSharing = shareState is ShareState.Loading
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (!isSharing && pages.isNotEmpty()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            .clickable(enabled = pages.isNotEmpty() && !isSharing) {
                                showShareSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint     = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text       = if (isSharing) "Preparando…" else "Compartir",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 15.sp,
                                color      = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Configuración del documento
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { showDocSettings = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Tune, "Configuración",
                            modifier = Modifier.size(20.dp),
                            tint     = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                        if (isLoading) {
                            items(6) { ShimmerCard(brush = shimmerBrush) }
                        } else {
                            itemsIndexed(pages) { index, page ->
                                PageCard(
                                    index        = index,
                                    path         = page.imagePath,
                                    version      = pageVersions[page.id] ?: 0L,
                                    shimmerBrush = shimmerBrush,
                                    onDelete     = { onDeletePage(page) },
                                    onEdit       = { onEditPage(page.id, page.imagePath) }
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

// ─── Share Bottom Sheet ───────────────────────────────────────────────────────

@Composable
private fun ShareBottomSheetContent(
    isLoading: Boolean,
    onSharePdf: () -> Unit,
    onShareImages: () -> Unit,
    onExportPdf: () -> Unit,
    onSign: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
    ) {
        Text(
            text       = "Exportar documento",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Text(
            text     = "Elige cómo quieres compartir o guardar el documento",
            style    = MaterialTheme.typography.bodySmall,
            color    = PlazoMuted,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        ShareSheetAction(
            icon     = Icons.Default.PictureAsPdf,
            iconTint = Color(0xFFD32F2F),
            iconBg   = Color(0x1AD32F2F),
            title    = "Compartir como PDF",
            subtitle = "Envía el documento en formato PDF",
            enabled  = !isLoading,
            onClick  = onSharePdf
        )
        ShareSheetAction(
            icon     = Icons.Default.PhotoLibrary,
            iconTint = Color(0xFF1565C0),
            iconBg   = Color(0x1A1565C0),
            title    = "Compartir como imágenes",
            subtitle = "Comparte cada página por separado",
            enabled  = !isLoading,
            onClick  = onShareImages
        )
        ShareSheetAction(
            icon     = Icons.Default.FileDownload,
            iconTint = Color(0xFF2E7D32),
            iconBg   = Color(0x1A2E7D32),
            title    = "Exportar páginas como PDF",
            subtitle = "Guarda el PDF en tu dispositivo",
            enabled  = !isLoading,
            onClick  = onExportPdf
        )

        HorizontalDivider(
            modifier  = Modifier.padding(vertical = 8.dp),
            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            thickness = 0.8.dp
        )

        ShareSheetAction(
            icon     = Icons.Default.Draw,
            iconTint = PlazoOlive,
            iconBg   = PlazoOlive.copy(alpha = 0.12f),
            title    = "Firmar documento",
            subtitle = "Añade tu firma al documento",
            enabled  = !isLoading,
            onClick  = onSign
        )
    }
}

@Composable
private fun ShareSheetAction(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg.copy(alpha = alpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint     = iconTint.copy(alpha = alpha),
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Text(
                subtitle,
                style  = MaterialTheme.typography.bodySmall,
                color  = PlazoMuted.copy(alpha = alpha),
                fontSize = 12.sp
            )
        }
    }
}

// ─── Shimmer ──────────────────────────────────────────────────────────────────

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1000f,
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

// ─── PageCard ─────────────────────────────────────────────────────────────────

@Composable
private fun PageCard(
    index: Int,
    path: String,
    version: Long = 0L,
    shimmerBrush: Brush,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val context  = LocalContext.current
    val cacheKey = "$path?v=$version"
    val request  = remember(cacheKey) {
        ImageRequest.Builder(context)
            .data(File(path))
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .size(600)
            .build()
    }
    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable { onEdit() }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Surface(
                    color    = MaterialTheme.colorScheme.primary,
                    shape    = CircleShape,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${index + 1}",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xCCE53935))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close, "Eliminar",
                        tint     = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
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
        Icon(
            Icons.Default.FilterFrames, null,
            modifier = Modifier.size(64.dp),
            tint     = PlazoMuted.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No hay imágenes",
            style = MaterialTheme.typography.titleMedium,
            color = PlazoMuted
        )
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
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt, null,
                modifier = Modifier.size(36.dp),
                tint     = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Cámara",
                style  = MaterialTheme.typography.bodySmall,
                color  = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
