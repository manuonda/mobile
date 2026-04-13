package com.nirv.converttopdf.ui.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme
import org.koin.androidx.compose.koinViewModel

// DrawSignatureScreen — canvas para crear una firma nueva.
// Estilo: Image 4 de referencia (CamScanner).
//
// Flujo:
//   Cancelar → onBack() sin guardar
//   Listo    → guarda en SignatureRepository vía DrawSignatureViewModel → onBack()
//
// @Preview usa DrawSignatureContent directamente (sin Koin).

@Composable
fun DrawSignatureScreen(
    onBack: () -> Unit,
    viewModel: DrawSignatureViewModel = koinViewModel()
) {
    DrawSignatureContent(
        onCancel = onBack,
        onDone = { strokes, size ->
            if (strokes.isNotEmpty() && size != IntSize.Zero) {
                val bitmap = renderStrokesToBitmap(strokes, size, android.graphics.Color.BLACK)
                viewModel.saveSignature(bitmap)
            }
            onBack()
        }
    )
}

// Colores disponibles para el trazo
private val strokeColors = listOf(
    Color.Black,
    Color.Blue,
    Color(0xFF1B5E20)  // verde oscuro
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawSignatureContent(
    onCancel: () -> Unit,
    onDone: (strokes: List<List<Offset>>, canvasSize: IntSize) -> Unit
) {
    var strokes      by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var undoHistory  by remember { mutableStateOf<List<List<List<Offset>>>>(emptyList()) }
    var redoHistory  by remember { mutableStateOf<List<List<List<Offset>>>>(emptyList()) }
    var canvasSize   by remember { mutableStateOf(IntSize.Zero) }
    var selectedColor by remember { mutableStateOf(Color.Black) }

    Scaffold(
        containerColor = Color(0xFF1C1C1C),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crear una firma",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Button(
                        onClick = { onDone(strokes, canvasSize) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BFA5)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Listo", color = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFF1C1C1C)) {
                // Ícono de herramienta actual
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Dibujar",
                    tint = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Text(
                    "Firme de forma clara y formal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )
                // Deshacer
                IconButton(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            redoHistory = redoHistory + listOf(strokes)
                            undoHistory = undoHistory.dropLast(1)
                            strokes = undoHistory.lastOrNull() ?: emptyList()
                        }
                    },
                    enabled = strokes.isNotEmpty()
                ) {
                    Text("↩", color = if (strokes.isNotEmpty()) Color.White else Color.Gray)
                }
                // Rehacer
                IconButton(
                    onClick = {
                        if (redoHistory.isNotEmpty()) {
                            val next = redoHistory.last()
                            redoHistory = redoHistory.dropLast(1)
                            undoHistory = undoHistory + listOf(strokes)
                            strokes = next
                        }
                    },
                    enabled = redoHistory.isNotEmpty()
                ) {
                    Text("↪", color = if (redoHistory.isNotEmpty()) Color.White else Color.Gray)
                }
                // Borrar todo
                IconButton(
                    onClick = {
                        undoHistory = undoHistory + listOf(strokes)
                        strokes = emptyList()
                        currentStroke = emptyList()
                        redoHistory = emptyList()
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Canvas principal — área de dibujo blanca
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                currentStroke = listOf(down.position)
                                do {
                                    val event = awaitPointerEvent()
                                    currentStroke = currentStroke + event.changes.map { it.position }
                                    event.changes.forEach { it.consume() }
                                } while (event.changes.any { it.pressed })
                                if (currentStroke.size >= 2) {
                                    undoHistory = undoHistory + listOf(strokes)
                                    redoHistory = emptyList()
                                    strokes = strokes + listOf(currentStroke)
                                }
                                currentStroke = emptyList()
                            }
                        }
                ) {
                    canvasSize = IntSize(size.width.toInt(), size.height.toInt())
                    val strokeStyle = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    (strokes + listOf(currentStroke)).filter { it.size >= 2 }.forEach { points ->
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, selectedColor, style = strokeStyle)
                    }
                }
                // Hint "Firme aquí" — solo visible cuando no hay trazos
                if (strokes.isEmpty() && currentStroke.isEmpty()) {
                    Text(
                        "Firme aquí",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Selector de color — columna derecha
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxSize()
                    .background(Color(0xFF2C2C2C)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                strokeColors.forEach { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }
        }
    }
}

// Renderiza los trazos a un Bitmap de Android recortado al bounding box.
//
// ¿Por qué recortar?
// El canvas de dibujo tiene el tamaño completo de la pantalla. Si guardamos
// ese bitmap completo y lo mostramos en el overlay (160×80dp), la firma
// aparece diminuta porque ocupa solo una fracción del bitmap total.
// Al recortar al bounding box de los trazos + un padding, el bitmap
// contiene solo la firma → se muestra a tamaño razonable en el overlay.
internal fun renderStrokesToBitmap(
    strokes: List<List<Offset>>,
    size: IntSize,
    color: Int = android.graphics.Color.BLACK
): Bitmap {
    val validStrokes = strokes.filter { it.size >= 2 }
    if (validStrokes.isEmpty()) {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    // Calcular el bounding box de todos los puntos dibujados
    val allPoints = validStrokes.flatten()
    val minX = allPoints.minOf { it.x }
    val minY = allPoints.minOf { it.y }
    val maxX = allPoints.maxOf { it.x }
    val maxY = allPoints.maxOf { it.y }

    // Añadimos 24px de margen para que el trazo no quede cortado en los bordes
    val pad = 24f
    val left   = (minX - pad).coerceAtLeast(0f)
    val top    = (minY - pad).coerceAtLeast(0f)
    val right  = (maxX + pad).coerceAtMost(size.width.toFloat())
    val bottom = (maxY + pad).coerceAtMost(size.height.toFloat())

    val cropW = (right - left).toInt().coerceAtLeast(1)
    val cropH = (bottom - top).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 6f
    }

    // Dibujamos desplazando las coordenadas al espacio recortado
    validStrokes.forEach { points ->
        val path = android.graphics.Path().apply {
            moveTo(points.first().x - left, points.first().y - top)
            points.drop(1).forEach { lineTo(it.x - left, it.y - top) }
        }
        canvas.drawPath(path, paint)
    }
    return bitmap
}

// ─── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
private fun DrawSignatureScreenPreview() {
    ConvertPngToPdfTheme {
        DrawSignatureContent(
            onCancel = {},
            onDone = { _, _ -> }
        )
    }
}
