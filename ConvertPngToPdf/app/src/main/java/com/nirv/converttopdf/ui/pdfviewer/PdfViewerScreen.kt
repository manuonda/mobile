package com.nirv.converttopdf.ui.pdfviewer

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nirv.converttopdf.ui.theme.PlazoMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerScreen(
    pdfPath: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var pages     by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var rawBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfPath) {
        isLoading = true
        errorMsg  = null
        withContext(Dispatchers.IO) {
            try {
                val pfd = openPdfDescriptor(context, pdfPath)
                if (pfd == null) {
                    withContext(Dispatchers.Main) {
                        errorMsg  = "No se encontró el archivo PDF.\nVerifica que el documento fue guardado correctamente."
                        isLoading = false
                    }
                    return@withContext
                }

                val renderer    = PdfRenderer(pfd)
                val screenWidth = context.resources.displayMetrics.widthPixels
                val bitmaps     = ArrayList<Bitmap>(renderer.pageCount)

                for (i in 0 until renderer.pageCount) {
                    val page   = renderer.openPage(i)
                    val scale  = screenWidth.toFloat() / page.width.toFloat()
                    val bmpW   = screenWidth
                    val bmpH   = (page.height * scale).toInt().coerceAtLeast(1)
                    val bmp    = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bmp)
                }
                renderer.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    rawBitmaps = bitmaps
                    pages      = bitmaps.map { it.asImageBitmap() }
                    isLoading  = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMsg  = "No se pudo renderizar el PDF:\n${e.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

    // Liberar bitmaps al salir de la pantalla
    DisposableEffect(pdfPath) {
        onDispose { rawBitmaps.forEach { it.recycle() } }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = title.ifBlank { "Documento PDF" },
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines   = 1
                        )
                        if (pages.isNotEmpty()) {
                            Text(
                                text  = "${pages.size} páginas",
                                style = MaterialTheme.typography.labelSmall,
                                color = PlazoMuted
                            )
                        }
                    }
                    if (!isLoading && errorMsg == null) {
                        IconButton(
                            onClick  = { sharePdf(context, pdfPath) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Share, "Compartir",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
                )
            }
        },
        containerColor = Color(0xFF1C1C1E)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color    = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Cargando PDF…",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                errorMsg != null -> {
                    Column(
                        modifier            = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.BrokenImage, null,
                            modifier = Modifier.size(64.dp),
                            tint     = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text      = errorMsg ?: "",
                            color     = Color.White.copy(alpha = 0.7f),
                            style     = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(onClick = onBack) { Text("Volver") }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier        = Modifier.fillMaxSize(),
                        contentPadding  = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(pages) { index, bitmap ->
                            Column {
                                // Número de página
                                Text(
                                    text     = "Página ${index + 1}",
                                    color    = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                )
                                Card(
                                    shape     = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier  = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        bitmap             = bitmap,
                                        contentDescription = "Página ${index + 1}",
                                        contentScale       = ContentScale.FillWidth,
                                        modifier           = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

private fun openPdfDescriptor(context: android.content.Context, path: String): ParcelFileDescriptor? =
    if (path.startsWith("content://")) {
        try { context.contentResolver.openFileDescriptor(Uri.parse(path), "r") }
        catch (_: Exception) { null }
    } else {
        val file = File(path)
        if (file.exists()) ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        else null
    }

private fun sharePdf(context: android.content.Context, path: String) {
    try {
        val uri = if (path.startsWith("content://")) {
            Uri.parse(path)
        } else {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", File(path))
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type     = "application/pdf"
            clipData = ClipData.newRawUri("PDF", uri)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir PDF").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    } catch (_: Exception) {}
}
