package com.nirv.converttopdf.ui.signature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme

// BottomSheet con las 3 opciones para añadir firma (Image 3 de referencia).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSignatureOptionsSheet(
    onCreateSignature: () -> Unit,
    onScanSignature: () -> Unit,
    onImportFromGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        AddSignatureOptionsContent(
            onCreateSignature = onCreateSignature,
            onScanSignature = onScanSignature,
            onImportFromGallery = onImportFromGallery,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun AddSignatureOptionsContent(
    onCreateSignature: () -> Unit,
    onScanSignature: () -> Unit,
    onImportFromGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Añadir Firma",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }

        HorizontalDivider()

        // Opción 1: Crear una firma
        OptionRow(
            icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(24.dp)) },
            label = "Crear una firma",
            onClick = onCreateSignature
        )

        // Opción 2: Escanear firma
        OptionRow(
            icon = { Icon(Icons.Default.Scanner, contentDescription = null, modifier = Modifier.size(24.dp)) },
            label = "Escanear firma",
            onClick = onScanSignature
        )

        // Opción 3: Importar desde galería
        OptionRow(
            icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(24.dp)) },
            label = "Importar desde la galería",
            onClick = onImportFromGallery
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OptionRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

// ─── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun AddSignatureOptionsSheetPreview() {
    ConvertPngToPdfTheme {
        AddSignatureOptionsContent(
            onCreateSignature = {},
            onScanSignature = {},
            onImportFromGallery = {},
            onDismiss = {}
        )
    }
}
