package com.nirv.converttopdf.ui.files

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.domain.model.PdfFile
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme
import com.nirv.converttopdf.ui.theme.PlazoMuted
import com.nirv.converttopdf.ui.theme.PlazoOlive
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// DirectryFileScreen — screen container
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DirectryFileScreen(
    viewModel: DirectoryViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val pdfFiles by viewModel.pdfFiles.collectAsState()
    val context  = LocalContext.current

    DirectoryPdfContent(
        pdfFiles   = pdfFiles,
        onBack     = onBack,
        onShare    = { file ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, file.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
        },
        onDelete         = { file -> viewModel.deleteFile(file) },
        onDeleteMultiple = { files -> files.forEach { viewModel.deleteFile(it) } },
        onShareMultiple  = { files ->
            val uris = ArrayList(files.map { it.uri })
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/pdf"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir PDFs"))
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DirectoryPdfContent — pure composable (Plazo style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DirectoryPdfContent(
    pdfFiles: List<PdfFile>,
    onBack: () -> Unit,
    onShare: (PdfFile) -> Unit,
    onDelete: (PdfFile) -> Unit,
    onDeleteMultiple: (List<PdfFile>) -> Unit,
    onShareMultiple: (List<PdfFile>) -> Unit
) {
    var searchQuery  by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf(setOf<Uri>()) }

    val filteredFiles = remember(pdfFiles, searchQuery) {
        if (searchQuery.isBlank()) pdfFiles
        else pdfFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val selectedFiles = pdfFiles.filter { it.uri in selectedUris }

    Scaffold(
        topBar = {
            // ── TopBar compacto con título centrado (Plazo style) ─────────────
            val topTitle = if (selectedUris.isEmpty()) "Mis Documentos"
                           else "${selectedUris.size} seleccionado${if (selectedUris.size > 1) "s" else ""}"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Izquierda: volver / deseleccionar
                IconButton(
                    onClick  = { if (selectedUris.isNotEmpty()) selectedUris = emptySet() else onBack() },
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
                    text       = topTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.align(Alignment.Center)
                )
                // Derecha: eliminar seleccionados
                if (selectedUris.isNotEmpty()) {
                    IconButton(
                        onClick  = { onDeleteMultiple(selectedFiles); selectedUris = emptySet() },
                        modifier = Modifier.size(40.dp).align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Delete,
                            contentDescription = "Eliminar seleccionados",
                            tint               = MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedUris.isNotEmpty(),
                enter   = slideInVertically { it },
                exit    = slideOutVertically { it }
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Compartir
                        Button(
                            onClick  = { onShareMultiple(selectedFiles); selectedUris = emptySet() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor   = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compartir", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        // Eliminar — CTA lima
                        Button(
                            onClick  = { onDeleteMultiple(selectedFiles); selectedUris = emptySet() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor   = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Eliminar", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Barra de búsqueda ─────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder   = { Text("Buscar documentos...", color = PlazoMuted, fontSize = 14.sp) },
                leadingIcon   = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = PlazoMuted, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                shape      = RoundedCornerShape(12.dp),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
                    focusedContainerColor   = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor             = MaterialTheme.colorScheme.primary
                )
            )

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onBackground,
                                modifier           = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text       = if (searchQuery.isBlank()) "Sin documentos aún"
                                         else "Sin resultados para \"$searchQuery\"",
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text  = if (searchQuery.isBlank()) "Los PDF que crees aparecerán aquí"
                                    else "Intenta con otro término de búsqueda",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PlazoMuted
                        )
                    }
                }
            } else {
                // ── Encabezado ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Recientes",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text  = "${filteredFiles.size} archivos",
                        style = MaterialTheme.typography.bodySmall,
                        color = PlazoMuted
                    )
                }

                // ── Lista con card contenedor estilo Plazo ────────────────────
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            filteredFiles.forEachIndexed { index, file ->
                                val isSelected = file.uri in selectedUris
                                PdfItem(
                                    file       = file,
                                    isSelected = isSelected,
                                    onToggle   = {
                                        selectedUris = if (isSelected)
                                            selectedUris - file.uri
                                        else
                                            selectedUris + file.uri
                                    },
                                    onShare  = { onShare(file) },
                                    onDelete = { onDelete(file) }
                                )
                                if (index < filteredFiles.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 90.dp),
                                        color    = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PdfItem — fila: thumbnail lila | nombre + fecha oliva | checkbox lima
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PdfItem(
    file: PdfFile,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(file.lastModified) {
        if (file.lastModified > 0L)
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified))
        else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail — fondo lila suave
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint               = Color(0xFFE53935),
                modifier           = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = file.name,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onBackground,
                maxLines   = 2,
                lineHeight = 18.sp
            )
            if (formattedDate.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = "$formattedDate  ·  ${file.size}",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = PlazoOlive,   // verde oliva — Plazo style
                    fontSize = 11.sp
                )
            } else if (file.size.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(text = file.size, style = MaterialTheme.typography.bodySmall, color = PlazoMuted)
            }
        }

        Spacer(Modifier.width(12.dp))

        // Checkbox — lima cuando seleccionado
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else PlazoMuted,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint               = MaterialTheme.colorScheme.onPrimary,
                    modifier           = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun DirectoryPreview() {
    val mockFiles = listOf(
        PdfFile("CIRCULAR-01-DPP-2026.pdf",        "", "1.5 MB", 1713213300000L, Uri.EMPTY),
        PdfFile("CamScanner 13-04-2026 18.35.pdf", "", "850 KB", 1712970360000L, Uri.EMPTY),
        PdfFile("Contrato_Alquiler_2026.pdf",      "", "2.1 MB", 1712884020000L, Uri.EMPTY)
    )
    ConvertPngToPdfTheme {
        DirectoryPdfContent(
            pdfFiles         = mockFiles,
            onShare          = {},
            onDelete         = {},
            onDeleteMultiple = {},
            onShareMultiple  = {},
            onBack           = {}
        )
    }
}
