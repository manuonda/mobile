package com.nirv.converttopdf.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.ui.theme.PlazoMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfOptionsBottomSheet(
    name: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
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
                text       = name,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                modifier   = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            Text(
                text     = "Archivo PDF exportado",
                style    = MaterialTheme.typography.bodySmall,
                color    = PlazoMuted,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            PdfSheetAction(
                icon     = Icons.Default.Share,
                iconTint = Color(0xFF1565C0),
                iconBg   = Color(0x1A1565C0),
                title    = "Compartir PDF",
                subtitle = "Envía el archivo por WhatsApp, email…",
                onClick  = onShare
            )
            PdfSheetAction(
                icon     = Icons.Default.OpenInNew,
                iconTint = Color(0xFF2E7D32),
                iconBg   = Color(0x1A2E7D32),
                title    = "Abrir PDF",
                subtitle = "Abre el archivo con otra aplicación",
                onClick  = onOpen
            )
        }
    }
}

@Composable
private fun PdfSheetAction(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PlazoMuted)
        }
    }
}
