package com.nirv.converttopdf.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeActionsGrid(
    onScanNew: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit
) {
    Column {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HomeActionItem(label = "Escanear", icon = Icons.Default.Add, onClick = onScanNew)
            HomeActionItem(label = "Importar\nimágenes", icon = Icons.Default.Image, onClick = onScanNew)
            HomeActionItem(label = "Mis PDFs", icon = Icons.Default.PictureAsPdf, onClick = onFiles)
            HomeActionItem(label = "Ajustes", icon = Icons.Default.Settings, onClick = onSettings)
        }
        Spacer(Modifier.height(28.dp))
    }
}
