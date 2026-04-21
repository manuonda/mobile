package com.nirv.converttopdf.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.domain.model.PdfFile
import com.nirv.converttopdf.ui.home.components.HomeActionsGrid
import com.nirv.converttopdf.ui.home.components.HomeHeader
import com.nirv.converttopdf.ui.home.components.RecentFileRow
import com.nirv.converttopdf.ui.home.components.RecentSectionHeader
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme
import com.nirv.converttopdf.ui.theme.PlazoMuted
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        draftDocs = draftDocs,
        onScanNew = onScanNew,
        onFiles = onFiles,
        onSettings = onSettings,
        onDraftClick = onDraftClick,
    )
}

@Composable
fun HomeScreenContent(
    draftDocs: List<DocumentEntity>,
    onScanNew: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit,
    onDraftClick: (Long) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }


    var filteredDrafts = remember(draftDocs, searchQuery){
        val sorted = draftDocs.sortedByDescending { it.createdAt }
        if(searchQuery.isBlank()) sorted.take(10) // Mostramos mas proyectos
        else sorted.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        /* item { HomeHeader() }
          item {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        } */

        item {
            HomeActionsGrid(
                onScanNew = onScanNew,
                onFiles = onFiles,
                onSettings = onSettings)
        }


        // Section Recientes
        item{
            RecentSectionHeader (
                onViewAll = onFiles
            )
        }

        if(filteredDrafts.isEmpty()) {
            item {
                EmptyRecentState(
                    isSearch = searchQuery.isNotBlank(),
                    searchQuery = searchQuery,
                    onScanNew = onScanNew
                )
            }
        } else {
            items(filteredDrafts) { draft ->
                DraftRow(
                    draft = draft,
                    onClick = { onDraftClick(draft.id)}
                )
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}





@Composable
private fun DraftRow(
    draft: DocumentEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${draft.name} #${draft.id}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "${draft.pageCount} imágenes • ${formatDate(draft.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = PlazoMuted
            )
        }
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Continuar",
            tint = PlazoMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyRecentState(
    isSearch: Boolean,
    searchQuery: String,
    onScanNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearch) Icons.Default.SearchOff else Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PlazoMuted.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isSearch) "No se encontraron resultados para \"$searchQuery\"" else "No hay archivos recientes",
            style = MaterialTheme.typography.bodyMedium,
            color = PlazoMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (!isSearch) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onScanNew,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Crear mi primer PDF")
            }
        }
    }
}



private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ConvertPngToPdfTheme {
        HomeScreenContent(
            draftDocs = listOf(
                DocumentEntity(id = 1, name = "Proyecto de prueba", pageCount = 3, createdAt = System.currentTimeMillis()),
                DocumentEntity(id = 2, name = "Otro documento", pageCount = 1, createdAt = System.currentTimeMillis())
            ),
            onScanNew = {},
            onFiles = {},
            onSettings = {},
            onDraftClick = {}
        )
    }
}
