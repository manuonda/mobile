package com.nirv.scansheet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector


@Composable
fun HomeScreen(
    onScanNew: () -> Unit,
    onFromGallery:()-> Unit,
    onSettings : () -> Unit

) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding( horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
             // Titular
            Text(
                text = "Escanear Documento",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC0392B),
                textAlign = TextAlign.Center
            )

            // Ilustración placeholder
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFFE8F4FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    modifier           = Modifier.size(80.dp),
                    tint               = Color(0xFF3DBFB8)
                )
            }

            Spacer(Modifier.height(40.dp))

            // Botones
            HomeButton(
                label   = "Escanear Nuevo",
                icon    = Icons.Default.DocumentScanner,
                color   = Color(0xFF3DBFB8),
                onClick = onScanNew
            )
            Spacer(Modifier.height(12.dp))
            HomeButton(
                label   = "Desde galería",
                icon    = Icons.Default.PhotoLibrary,
                color   = Color(0xFF4A86D9),
                onClick = onFromGallery
            )
            Spacer(Modifier.height(12.dp))
            HomeButton(
                label   = "Ajustes",
                icon    = Icons.Default.Settings,
                color   = Color(0xFFE8795A),
                onClick = onSettings
            )
        }
    }
}


@Composable
fun HomeButton(
    label:   String,
    icon:    ImageVector,
    color:   Color,
    onClick: () -> Unit,
) {
    Button(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(26.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        onScanNew = {},
        onFromGallery = {},
        onSettings = {}
    )
}