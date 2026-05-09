package com.nirv.converttopdf.ui.files

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectryFileScreen(
    onBack: () -> Unit,
    onDraftClick: (Long) -> Unit = {},
    viewModel: FilesViewModel = koinViewModel()
) {
    val allDocs      by viewModel.allDocs.collectAsState()
    val recentDocs   by viewModel.recentDocs.collectAsState()
    val favoriteDocs by viewModel.favoriteDocs.collectAsState()
    val firstPages   by viewModel.firstPages.collectAsState()
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()

    var searchQuery   by remember { mutableStateOf("") }
    var selectedTab   by remember { mutableStateOf(FilesTab.TODOS) }
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
            onOpen     = {
                scope.launch { pdfSheetState.hide() }.invokeOnCompletion {
                    selectedPdf = null
                    pdf.pdfPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                clipData = ClipData.newRawUri("PDF", uri)
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Abrir PDF").apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        }
                    }
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
                    Text(
                        "Archivos",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    Text(
                        "${filteredList.size} archivos",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = PlazoMuted,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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

            if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredList, key = { it.id }) { doc ->
                        FileRow(
                            doc              = doc,
                            firstPagePath    = firstPages[doc.id],
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
                    item { Spacer(Modifier.height(24.dp)) }
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
