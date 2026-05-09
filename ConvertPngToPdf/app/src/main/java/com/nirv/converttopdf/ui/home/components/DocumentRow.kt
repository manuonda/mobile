package com.nirv.converttopdf.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.data.db.entity.DocumentEntity
import com.nirv.converttopdf.data.db.entity.DocumentType
import com.nirv.converttopdf.ui.theme.PlazoMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentRow(
    doc: DocumentEntity,
    onClick: () -> Unit
) {
    val isPdf = doc.type == DocumentType.EXPORTED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Icono izquierdo ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isPdf) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Edit,
                contentDescription = null,
                tint               = if (isPdf) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // ── Nombre y badge ───────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = doc.name,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isPdf) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text       = if (isPdf) "PDF" else "Proyecto",
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isPdf) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.secondary,
                        modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text  = if (isPdf) "Archivo PDF  •  ${formatDocDate(doc.createdAt)}"
                        else "${doc.pageCount} ${if (doc.pageCount == 1) "página" else "páginas"}  •  ${formatDocDate(doc.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = PlazoMuted
            )
        }

        // ── Icono derecho ─────────────────────────────────────────────────────
        Icon(
            imageVector        = if (isPdf) Icons.Default.MoreVert else Icons.Default.ChevronRight,
            contentDescription = null,
            tint               = PlazoMuted,
            modifier           = Modifier.size(20.dp)
        )
    }
}

internal fun formatDocDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
