package com.nirv.converttopdf.ui.files.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.ui.files.SortDirection
import com.nirv.converttopdf.ui.files.SortField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    sheetState:       SheetState,
    currentField:     SortField,
    currentDirection: SortDirection,
    onFieldChange:    (SortField)     -> Unit,
    onDirectionChange:(SortDirection) -> Unit,
    onDismiss:        () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text       = "Ordenar por",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

            Spacer(Modifier.height(8.dp))

            SortField.entries.forEach { field ->
                SortRadioRow(
                    label     = field.label,
                    selected  = currentField == field,
                    onClick   = { onFieldChange(field) }
                )
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier  = Modifier.padding(vertical = 8.dp)
            )

            SortDirection.entries.forEach { dir ->
                SortRadioRow(
                    label    = dir.label,
                    selected = currentDirection == dir,
                    onClick  = { onDirectionChange(dir) }
                )
            }
        }
    }
}

@Composable
private fun SortRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick  = onClick,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
