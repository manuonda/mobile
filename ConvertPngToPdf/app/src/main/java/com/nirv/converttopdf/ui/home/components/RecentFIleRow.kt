package com.nirv.converttopdf.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.domain.model.PdfFile
import com.nirv.converttopdf.ui.theme.PlazoMuted
import com.nirv.converttopdf.ui.theme.PlazoOlive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ui/home/components/RecentFileRow.kt
@Composable
fun RecentFileRow(
    file: PdfFile,
    onClick: () -> Unit
) {
    val formattedDate = remember(file.lastModified) {
        val date = Date(file.lastModified)
        val now = Date()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        if (sdf.format(date) == sdf.format(now)) {
            "Hoy, " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(date)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PlazoOlive.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = PlazoOlive,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = "$formattedDate  ·  ${file.size}",
                fontSize = 11.sp,
                color = PlazoMuted
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = PlazoMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}
