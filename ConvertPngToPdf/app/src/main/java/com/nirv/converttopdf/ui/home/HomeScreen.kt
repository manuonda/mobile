package com.nirv.converttopdf.ui.home

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentType
import com.nirv.converttopdf.ui.home.components.*
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun HomeScreen(
    onScanNew: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit,
    onDraftClick: (Long) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val draftDocs by viewModel.draftDocuments.collectAsState()

    HomeScreenContent(
        draftDocs    = draftDocs,
        onScanNew    = onScanNew,
        onFiles      = onFiles,
        onSettings   = onSettings,
        onDraftClick = onDraftClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    draftDocs: List<DocumentEntity>,
    onScanNew: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit,
    onDraftClick: (Long) -> Unit,
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    var searchQuery   by remember { mutableStateOf("") }
    var selectedPdf   by remember { mutableStateOf<DocumentEntity?>(null) }
    val pdfSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredDrafts = remember(draftDocs, searchQuery) {
        val sorted = draftDocs.sortedByDescending { it.createdAt }
        if (searchQuery.isBlank()) sorted.take(10)
        else sorted.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // ── Bottom sheet PDF ──────────────────────────────────────────────────────
    selectedPdf?.let { pdf ->
        PdfOptionsBottomSheet(
            name       = pdf.name,
            sheetState = pdfSheetState,
            onDismiss  = { selectedPdf = null },
            onShare    = {
                scope.launch { pdfSheetState.hide() }.invokeOnCompletion {
                    selectedPdf = null
                    pdf.pdfPath?.let { path ->
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", File(path)
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type     = "application/pdf"
                            clipData = ClipData.newRawUri("PDF", uri)
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Compartir PDF").apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        )
                    }
                }
            },
            onOpen     = {
                scope.launch { pdfSheetState.hide() }.invokeOnCompletion {
                    selectedPdf = null
                    pdf.pdfPath?.let { path ->
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", File(path)
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            clipData = ClipData.newRawUri("PDF", uri)
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(intent, "Abrir PDF").apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        )
                    }
                }
            }
        )
    }

    // ── Contenido ─────────────────────────────────────────────────────────────
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { HomeHeader(documentCount = draftDocs.size) }

        item {
            HomeActionsGrid(
                onScanNew  = onScanNew,
                onFiles    = onFiles,
                onSettings = onSettings
            )
        }

        item { RecentSectionHeader(onViewAll = onFiles) }

        if (filteredDrafts.isEmpty()) {
            item {
                EmptyRecentState(
                    isSearch    = searchQuery.isNotBlank(),
                    searchQuery = searchQuery,
                    onScanNew   = onScanNew
                )
            }
        } else {
            items(filteredDrafts, key = { it.id }) { doc ->
                DocumentRow(
                    doc     = doc,
                    onClick = {
                        if (doc.type == DocumentType.EXPORTED) selectedPdf = doc
                        else onDraftClick(doc.id)
                    }
                )
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ConvertPngToPdfTheme {
        HomeScreenContent(
            draftDocs = listOf(
                DocumentEntity(
                    id        = 1,
                    name      = "Proyecto de prueba",
                    pageCount = 3,
                    createdAt = System.currentTimeMillis(),
                    type      = DocumentType.PROJECT
                ),
                DocumentEntity(
                    id        = 2,
                    name      = "Factura_Mayo.pdf",
                    pageCount = 0,
                    createdAt = System.currentTimeMillis(),
                    type      = DocumentType.EXPORTED,
                    pdfPath   = "/data/user/0/com.nirv.converttopdf/files/Factura_Mayo.pdf"
                )
            ),
            onScanNew    = {},
            onFiles      = {},
            onSettings   = {},
            onDraftClick = {},
        )
    }
}
