package com.nirv.converttopdf.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nirv.converttopdf.ui.theme.PlazoMuted

@Composable
fun EmptyRecentState(
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
            imageVector        = if (isSearch) Icons.Default.SearchOff else Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier           = Modifier.size(64.dp),
            tint               = PlazoMuted.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text      = if (isSearch) "No se encontraron resultados para \"$searchQuery\""
                        else "No hay archivos recientes",
            style     = MaterialTheme.typography.bodyMedium,
            color     = PlazoMuted,
            textAlign = TextAlign.Center
        )
        if (!isSearch) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onScanNew, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Crear mi primer PDF")
            }
        }
    }
}
