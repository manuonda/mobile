package com.nirv.converttopdf.ui.files.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentType
import com.nirv.converttopdf.ui.theme.PlazoMuted
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileRow(
    doc: DocumentEntity,
    firstPagePath: String?,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val isPdf = doc.type == DocumentType.EXPORTED
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val subtitle = buildString {
        if (isPdf) {
            append("PDF exportado")
            doc.pdfPath?.let { path ->
                val size = File(path).length()
                if (size > 0) append("  •  ${formatFileSize(size)}")
            }
        } else {
            append("${doc.pageCount} ${if (doc.pageCount == 1) "página" else "páginas"}")
        }
        append("  •  ${formatFileDate(doc.createdAt)}")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isPdf) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isPdf && firstPagePath != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(firstPagePath))
                        .size(104)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                )
            } else {
                Icon(
                    imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (isPdf) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = doc.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isPdf) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (isPdf) "PDF" else "Proyecto",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPdf) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = PlazoMuted)
        }

        IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (doc.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (doc.isFavorite) "Quitar favorito" else "Añadir a favoritos",
                tint = if (doc.isFavorite) MaterialTheme.colorScheme.error else PlazoMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert, "Más opciones",
                    tint = PlazoMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

private fun formatFileDate(ts: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(ts))

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024     -> "%.0f KB".format(bytes / 1_024.0)
    else               -> "$bytes B"
}
