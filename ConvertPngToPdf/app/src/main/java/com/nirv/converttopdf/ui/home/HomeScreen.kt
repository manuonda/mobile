package com.nirv.converttopdf.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.domain.model.PdfFile
import com.nirv.converttopdf.ui.files.DirectoryViewModel
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme
import com.nirv.converttopdf.ui.theme.PlazoMuted
import com.nirv.converttopdf.ui.theme.PlazoOlive
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — Plazo-style layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onScanNew: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit,
    viewModel: DirectoryViewModel = koinViewModel()
) {
    val pdfFiles by viewModel.pdfFiles.collectAsState()
    val recentFiles = remember(pdfFiles) {
        pdfFiles.sortedByDescending { it.lastModified }.take(6)
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredRecent = remember(recentFiles, searchQuery) {
        if (searchQuery.isBlank()) recentFiles
        else recentFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = "PDF Converter",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint               = MaterialTheme.colorScheme.onBackground,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Barra de búsqueda ─────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                placeholder   = {
                    Text("Buscar documentos...", color = PlazoMuted, fontSize = 14.sp)
                },
                leadingIcon   = {
                    Icon(Icons.Default.Search, null, tint = PlazoMuted, modifier = Modifier.size(18.dp))
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
        }

        // ── Grid de acciones (estilo Plazo: círculos lila) ────────────────────
        item {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionItem(
                    label   = "Escanear",
                    icon    = Icons.Default.Add,
                    onClick = onScanNew
                )
                ActionItem(
                    label   = "Importar\nimágenes",
                    icon    = Icons.Default.Image,
                    onClick = onScanNew
                )
                ActionItem(
                    label   = "Mis PDFs",
                    icon    = Icons.Default.PictureAsPdf,
                    onClick = onFiles
                )
                ActionItem(
                    label   = "Ajustes",
                    icon    = Icons.Default.Settings,
                    onClick = onSettings
                )
            }
            Spacer(Modifier.height(28.dp))
        }

        // ── Sección "Recientes" ───────────────────────────────────────────────
        item {
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
                    fontSize   = 18.sp,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clickable { onFiles() }
                ) {
                    Text(
                        text  = "Ver todo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlazoMuted
                    )
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint               = PlazoMuted,
                        modifier           = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Lista de archivos recientes ───────────────────────────────────────
        if (filteredRecent.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
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
                            text  = if (searchQuery.isBlank()) "Aún no has creado PDFs"
                                    else "Sin resultados para \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (searchQuery.isBlank()) {
                            Text(
                                text       = "Toca Escanear para empezar",
                                color      = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium,
                                style      = MaterialTheme.typography.bodyMedium,
                                modifier   = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onScanNew() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // ── Card contenedor estilo Plazo ──────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    filteredRecent.forEachIndexed { index, file ->
                        RecentFileRow(
                            file    = file,
                            onClick = onFiles
                        )
                        if (index < filteredRecent.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 88.dp),
                                color    = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ActionItem — círculo lila con icono + label (estilo Plazo)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),  // lavanda Plazo
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = MaterialTheme.colorScheme.onBackground,
                modifier           = Modifier.size(26.dp)
            )
        }
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RecentFileRow — fila con thumbnail + info + checkbox estilo Plazo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentFileRow(
    file: PdfFile,
    onClick: () -> Unit
) {
    var checked by remember { mutableStateOf(false) }

    val formattedDate = remember(file.lastModified) {
        if (file.lastModified > 0L)
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified))
        else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail PDF — fondo lila suave
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint               = Color(0xFFE53935),
                modifier           = Modifier.size(30.dp)
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
            if (formattedDate.isNotEmpty() || file.size.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        if (formattedDate.isNotEmpty()) append(formattedDate)
                        if (formattedDate.isNotEmpty() && file.size.isNotEmpty()) append("  ·  ")
                        if (file.size.isNotEmpty()) append(file.size)
                    },
                    style    = MaterialTheme.typography.bodySmall,
                    color    = PlazoOlive,   // fecha en verde oliva (Plazo style)
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Checkbox — verde lima cuando seleccionado
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
                )
                .border(
                    width = 1.5.dp,
                    color = if (checked) MaterialTheme.colorScheme.primary else PlazoMuted,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { checked = !checked },
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
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
fun HomeScreenPreview() {
    ConvertPngToPdfTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("PDF Converter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), Arrangement.SpaceEvenly) {
                    ActionItem("Escanear",           Icons.Default.Add,          {})
                    ActionItem("Importar\nimágenes", Icons.Default.Image,        {})
                    ActionItem("Mis PDFs",           Icons.Default.PictureAsPdf, {})
                    ActionItem("Ajustes",            Icons.Default.Settings,     {})
                }
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Recientes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Ver todo", color = PlazoMuted)
                }
                Spacer(Modifier.height(8.dp))
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    listOf(
                        PdfFile("CIRCULAR-01-DPP-2026.pdf",          "", "1.5 MB", 1713213300000L, android.net.Uri.EMPTY),
                        PdfFile("CamScanner 13-04-2026 18.35.pdf",   "", "850 KB", 1712970360000L, android.net.Uri.EMPTY)
                    ).forEachIndexed { i, file ->
                        RecentFileRow(file = file, onClick = {})
                        if (i == 0) HorizontalDivider(Modifier.padding(start = 88.dp), color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
