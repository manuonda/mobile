package com.nirv.converttopdf.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta de colores igual a la imagen de referencia
private val ColorTeal   = Color(0xFF4DD0C4)
private val ColorBlue   = Color(0xFF5B8BF5)
private val ColorOrange = Color(0xFFF07A5C)
private val ColorTitle  = Color(0xFF8B1A1A)   // rojo oscuro del título

@Composable
fun HomeScreen(
    onScanNew: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = Color(0xFFF5F5F5)          // fondo gris muy claro
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Título principal ──────────────────────────────────────────────
            Text(
                text       = "Conversor de imagen a PDF",
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = ColorTitle,
                textAlign  = TextAlign.Center,
                lineHeight = 40.sp
            )

            Spacer(Modifier.height(40.dp))

            // ── Ilustración hero ──────────────────────────────────────────────
            HeroIllustration()

            Spacer(Modifier.height(48.dp))

            // ── Botones de acción ─────────────────────────────────────────────
            HomeActionButton(
                label   = "Crear nuevo PDF",
                icon    = Icons.Default.Add,
                color   = ColorTeal,
                onClick = onScanNew
            )

            Spacer(Modifier.height(20.dp))

            HomeActionButton(
                label   = "Archivo PDF",
                icon    = Icons.Default.Description,
                color   = ColorBlue,
                onClick = onFiles
            )

            Spacer(Modifier.height(20.dp))

            HomeActionButton(
                label   = "Ajustes",
                icon    = Icons.Default.Settings,
                color   = ColorOrange,
                onClick = onSettings
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HomeActionButton — botón pill con icono a la derecha y sombra inferior
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    // shadow() debe aplicarse ANTES de clip() para que se vea correctamente
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation        = 8.dp,
                shape            = RoundedCornerShape(16.dp),
                ambientColor     = color.copy(alpha = 0.3f),
                spotColor        = color.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = label,
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp
            )
            // Círculo con icono a la derecha
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HeroIllustration — placeholder decorativo hasta tener el asset real
// Simula la silueta de la ilustración de la referencia con capas de íconos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroIllustration() {
    Box(
        modifier         = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Color(0xFFE3F2FD)),
        contentAlignment = Alignment.Center
    ) {
        // Capa de fondo decorativa
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFBBDEFB).copy(alpha = 0.5f))
        )
        // Icono principal
        Icon(
            imageVector        = Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier           = Modifier.size(96.dp),
            tint               = Color(0xFF0277BD)
        )
        // Badge teal en esquina inferior derecha
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorTeal),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            onScanNew  = {},
            onFiles    = {},
            onSettings = {}
        )
    }
}
