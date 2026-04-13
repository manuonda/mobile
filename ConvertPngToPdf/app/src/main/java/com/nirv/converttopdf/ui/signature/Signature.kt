package com.nirv.converttopdf.ui.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme
import org.koin.androidx.compose.koinViewModel

// SignatureScreen — pantalla donde el usuario dibuja su firma con el dedo.
//
// Separación de responsabilidades:
//   • Screen  → maneja el dibujo (List<List<Offset>>) con estado local (remember)
//   • ViewModel → recibe el Bitmap final y lo superpone sobre la última imagen
//
// Así el ViewModel no depende de ningún tipo de Compose.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(
    onBack: () -> Unit,
    viewModel: SignatureViewModel = koinViewModel()
) {
    // Trazos completos: cada trazo es una lista de puntos (Offset = punto x,y en pantalla)
    // 'remember' conserva el valor entre recomposiciones sin reinicializarlo
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }

    // Trazo actual mientras el dedo está en movimiento
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Tamaño real del Canvas necesario para crear el Bitmap de la firma
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firmar Documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                // Limpiar: borra todos los trazos del canvas
                OutlinedButton(
                    onClick = {
                        strokes = emptyList()
                        currentStroke = emptyList()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text("Limpiar")
                }

                // Aplicar: convierte los trazos a Bitmap y los envía al ViewModel
                Button(
                    onClick = {
                        if (strokes.isNotEmpty() && canvasSize != IntSize.Zero) {
                            val bitmap = renderStrokesToBitmap(strokes, canvasSize)
                            viewModel.applySignature(bitmap)
                        }
                        onBack()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text("Aplicar")
                }
            }
        }
    ) { padding ->

        // Canvas de Compose: área de dibujo táctil con fondo blanco
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                // pointerInput captura los gestos táctiles del usuario
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // 1. El usuario toca la pantalla → iniciamos un nuevo trazo
                        val down = awaitFirstDown()
                        currentStroke = listOf(down.position)

                        // 2. Seguimos cada movimiento del dedo
                        do {
                            val event = awaitPointerEvent()
                            val newPoints = event.changes.map { it.position }
                            currentStroke = currentStroke + newPoints
                            // consume() evita que otros elementos reciban el evento
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })

                        // 3. El dedo se levantó → guardamos el trazo completo
                        if (currentStroke.size >= 2) {
                            strokes = strokes + listOf(currentStroke)
                        }
                        currentStroke = emptyList()
                    }
                }
        ) {
            // Guardamos el tamaño del canvas para usarlo al crear el Bitmap
            canvasSize = IntSize(size.width.toInt(), size.height.toInt())

            val strokeStyle = Stroke(
                width = 6f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // Dibujamos todos los trazos guardados + el trazo en curso
            (strokes + listOf(currentStroke))
                .filter { it.size >= 2 }
                .forEach { points ->
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { offset -> lineTo(offset.x, offset.y) }
                    }
                    drawPath(path = path, color = Color.Black, style = strokeStyle)
                }
        }
    }
}

// Convierte la lista de trazos (Compose Offsets) a un android.graphics.Bitmap.
// Función pura: no tiene estado, no conoce Compose más allá de los tipos básicos.
private fun renderStrokesToBitmap(
    strokes: List<List<Offset>>,
    size: IntSize
): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 6f
    }

    strokes.filter { it.size >= 2 }.forEach { points ->
        val path = android.graphics.Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { offset -> lineTo(offset.x, offset.y) }
        }
        canvas.drawPath(path, paint)
    }

    return bitmap
}

@Preview(showBackground = true)
@Composable
fun SignaturePreview() {
    ConvertPngToPdfTheme {
        // En la Preview no usamos Koin, pasamos un onBack vacío.
        // Nota: Como SignatureScreen usa koinViewModel() por defecto,
        // para la preview lo ideal sería que SignatureScreen aceptara el ViewModel como parámetro
        // o usar un CompositionLocal para el ViewModel.
        // Pero para visualizar el layout básico:
        SignatureScreenContent(onBack = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreenContent(
    onBack: () -> Unit,
    onApply: (List<List<Offset>>, IntSize) -> Unit = { _, _ -> }
) {
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firmar Documento (Preview)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                OutlinedButton(
                    onClick = {
                        strokes = emptyList()
                        currentStroke = emptyList()
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text("Limpiar")
                }
                Button(
                    onClick = { onApply(strokes, canvasSize) },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text("Aplicar")
                }
            }
        }
    ) { padding ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        currentStroke = listOf(down.position)
                        do {
                            val event = awaitPointerEvent()
                            currentStroke = currentStroke + event.changes.map { it.position }
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        if (currentStroke.size >= 2) strokes = strokes + listOf(currentStroke)
                        currentStroke = emptyList()
                    }
                }
        ) {
            canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            val strokeStyle = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            (strokes + listOf(currentStroke)).filter { it.size >= 2 }.forEach { points ->
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { offset -> lineTo(offset.x, offset.y) }
                }
                drawPath(path = path, color = Color.Black, style = strokeStyle)
            }
        }
    }
}
