package com.nirv.scansheet.ui.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nirv.scansheet.domain.model.ExcelRow
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    viewModel: PreviewViewModel = koinViewModel()
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revisar y editar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = onExport,
                enabled = rows.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Generar Excel (${rows.size} filas)")
            }
        }
    ) { padding ->
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No se reconoció texto.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${rows.size} filas — podés editar cualquier celda antes de exportar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                }
                itemsIndexed(rows) { rowIndex, row ->
                    EditableRow(
                        rowIndex = rowIndex,
                        row = row,
                        onCellChanged = { colIndex, value ->
                            viewModel.updateCell(rowIndex, colIndex, value)
                        },
                        onDelete = { viewModel.deleteRow(rowIndex) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun EditableRow(
    rowIndex: Int,
    row: ExcelRow,
    onCellChanged: (colIndex: Int, value: String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número de fila
            Text(
                text = "${rowIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )

            // Celdas editables
            Column(modifier = Modifier.weight(1f)) {
                row.columns.forEachIndexed { colIndex, cell ->
                    // Estado local para que el teclado sea fluido
                    var text by remember(cell) { mutableStateOf(cell) }

                    OutlinedTextField(
                        value = text,
                        onValueChange = { newValue ->
                            text = newValue
                            onCellChanged(colIndex, newValue)
                        },
                        label = {
                            Text(if (colIndex == 0) "Descripción" else "Valor")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (colIndex == 0) KeyboardType.Text
                                           else KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (colIndex < row.columns.size - 1) {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // Botón eliminar fila
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar fila",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
