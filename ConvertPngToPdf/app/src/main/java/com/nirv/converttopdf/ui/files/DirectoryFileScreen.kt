package com.nirv.converttopdf.ui.files

import android.content.ClipData
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentType
import com.nirv.converttopdf.ui.files.components.FileRow
import com.nirv.converttopdf.ui.files.components.SortBottomSheet
import com.nirv.converttopdf.ui.home.components.PdfOptionsBottomSheet
import com.nirv.converttopdf.ui.theme.PlazoMuted
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

enum class FilesTab(val label: String) {
    TODOS("Todos"),
    RECIENTES("Recientes"),
    FAVORITOS("Favoritos")
}

enum class SortField(val label: String) {
    FECHA("Fecha"),
    NOMBRE("Nombre"),
    TAMAÑO("Tamaño del archivo")
}

enum class SortDirection(val label: String) {
    ASCENDENTE("Ascendente"),
    DESCENDENTE("Descendente")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectryFileScreen(
    onBack: () -> Unit,
    onDraftClick: (Long) -> Unit = {},
    onPdfOpen: (pdfPath: String, title: String) -> Unit = { _, _ -> },
    viewModel: FilesViewModel = koinViewModel()
) {
    val allDocs      by viewModel.allDocs.collectAsState()
    val recentDocs   by viewModel.recentDocs.collectAsState()
    val favoriteDocs by viewModel.favoriteDocs.collectAsState()
    val firstPages   by viewModel.firstPages.collectAsState()
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()

    // Search & tab
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(FilesTab.TODOS) }

    // Sort
    var sortField     by remember { mutableStateOf(SortField.FECHA) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESCENDENTE) }
    var showSortSheet by remember { mutableStateOf(false) }
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Selection mode
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds   by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // PDF options sheet
    var selectedPdf   by remember { mutableStateOf<DocumentEntity?>(null) }
    val pdfSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeList = when (selectedTab) {
        FilesTab.TODOS     -> allDocs
        FilesTab.RECIENTES -> recentDocs
        FilesTab.FAVORITOS -> favoriteDocs
    }

    val filteredList = remember(activeList, searchQuery) {
        if (searchQuery.isBlank()) activeList
        else activeList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val sortedList = remember(filteredList, sortField, sortDirection) {
        val base = when (sortField) {
            SortField.FECHA  -> filteredList.sortedBy { it.createdAt }
            SortField.NOMBRE -> filteredList.sortedBy { it.name.lowercase() }
            SortField.TAMAÑO -> filteredList.sortedBy {
                it.pdfPath?.let { p -> File(p).takeIf { f -> f.exists() }?.length() } ?: 0L
            }
        }
        if (sortDirection == SortDirection.DESCENDENTE) base.reversed() else base
    }

    // Sort bottom sheet
    if (showSortSheet) {
        SortBottomSheet(
            sheetState        = sortSheetState,
            currentField      = sortField,
            currentDirection  = sortDirection,
            onFieldChange     = { sortField = it },
            onDirectionChange = { sortDirection = it },
            onDismiss         = { showSortSheet = false }
        )
    }

    // PDF options sheet
    selectedPdf?.let { pdf ->
        PdfOptionsBottomSheet(
            name       = pdf.name,
            sheetState = pdfSheetState,
            onDismiss  = { selectedPdf = null },
            onShare    = {
                scope.launch { pdfSheetState.hide() }.invokeOnCompletion {
                    selectedPdf = null
                    pdf.pdfPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type     = "application/pdf"
                                clipData = ClipData.newRawUri("PDF", uri)
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir PDF").apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        }
                    }
                }
            },
            onOpen = {
                scope.launch { pdfSheetState.hide() }.invokeOnCompletion {
                    selectedPdf = null
                    pdf.pdfPath?.let { path -> onPdfOpen(path, pdf.name) }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectionMode) {
                        // Selection mode header
                        IconButton(
                            onClick  = { selectionMode = false; selectedIds = emptySet() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, "Cancelar selección", modifier = Modifier.size(22.dp))
                        }
                        Text(
                            text       = if (selectedIds.isEmpty()) "Seleccionar" else "${selectedIds.size} seleccionados",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.weight(1f)
                        )
                    } else {
                        // Normal header
                        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", modifier = Modifier.size(22.dp))
                        }
                        Text(
                            text       = "Archivos",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.weight(1f)
                        )
                        Text(
                            "${sortedList.size} archivos",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = PlazoMuted,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        // Sort icon
                        IconButton(
                            onClick  = { showSortSheet = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Ordenar",
                                tint     = if (sortField != SortField.FECHA || sortDirection != SortDirection.DESCENDENTE)
                                               MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        // Select mode icon
                        IconButton(
                            onClick  = { selectionMode = true; selectedIds = emptySet() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckBox,
                                contentDescription = "Seleccionar",
                                tint     = PlazoMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    placeholder   = { Text("Buscar documentos…", color = PlazoMuted, fontSize = 14.sp) },
                    leadingIcon   = {
                        Icon(Icons.Default.Search, null, tint = PlazoMuted, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(12.dp),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
                        focusedContainerColor   = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (!selectionMode) {
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor   = MaterialTheme.colorScheme.surface,
                        contentColor     = MaterialTheme.colorScheme.primary
                    ) {
                        FilesTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick  = { selectedTab = tab; searchQuery = "" },
                                text     = {
                                    Text(
                                        tab.label,
                                        fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize   = 13.sp
                                    )
                                }
                            )
                        }
                    }
                }

                if (sortedList.isEmpty()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Folder, null,
                                modifier = Modifier.size(64.dp),
                                tint     = PlazoMuted.copy(alpha = 0.3f)
                            )
                            Text(
                                text       = if (searchQuery.isBlank()) emptyMessage(selectedTab)
                                             else "Sin resultados para \"$searchQuery\"",
                                fontWeight = FontWeight.SemiBold,
                                color      = PlazoMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(sortedList, key = { it.id }) { doc ->
                            FileRow(
                                doc              = doc,
                                firstPagePath    = firstPages[doc.id],
                                isSelectionMode  = selectionMode,
                                isSelected       = doc.id in selectedIds,
                                onToggleSelect   = {
                                    selectedIds = if (doc.id in selectedIds)
                                        selectedIds - doc.id else selectedIds + doc.id
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                                onDelete         = { viewModel.deleteDocument(doc) },
                                onClick          = {
                                    if (doc.type == DocumentType.EXPORTED) selectedPdf = doc
                                    else onDraftClick(doc.id)
                                }
                            )
                            HorizontalDivider(
                                modifier  = Modifier.padding(start = 80.dp, end = 16.dp),
                                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }

            // Bulk delete bar (slides up when in selection mode)
            AnimatedVisibility(
                visible = selectionMode,
                enter   = slideInVertically { it },
                exit    = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color       = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier    = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Select All / Deselect All
                        OutlinedButton(
                            onClick  = {
                                selectedIds = if (selectedIds.size == sortedList.size)
                                    emptySet() else sortedList.map { it.id }.toSet()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text     = if (selectedIds.size == sortedList.size) "Deseleccionar" else "Selec. todo",
                                maxLines = 1
                            )
                        }
                        // Delete button
                        Button(
                            onClick  = {
                                val toDelete = sortedList.filter { it.id in selectedIds }
                                toDelete.forEach { viewModel.deleteDocument(it) }
                                selectedIds   = emptySet()
                                selectionMode = false
                            },
                            enabled  = selectedIds.isNotEmpty(),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text     = if (selectedIds.isEmpty()) "Eliminar" else "Eliminar (${selectedIds.size})",
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun emptyMessage(tab: FilesTab): String = when (tab) {
    FilesTab.TODOS     -> "No hay documentos aún"
    FilesTab.RECIENTES -> "No hay documentos recientes"
    FilesTab.FAVORITOS -> "No tienes favoritos todavía"
}
