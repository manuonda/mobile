package com.nirv.converttopdf.ui.signature

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// SignatureScreen
//
// Muestra el documento (última imagen) y permite:
//   • Seleccionar / importar una firma guardada
//   • Arrastrar la firma sobre el documento (zona central del overlay)
//   • Redimensionar arrastrando la esquina inferior-derecha
//   • Cambiar el color de tinta (panel "Editar" — esquina superior-derecha)
//   • Confirmar → bake de la firma en el bitmap
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignatureScreen(
    onBack: () -> Unit,
    onDrawNew: () -> Unit,
    viewModel: SignatureViewModel = koinViewModel()
) {
    val images          by viewModel.images.collectAsStateWithLifecycle()
    val savedSignatures by viewModel.savedSignatures.collectAsStateWithLifecycle()
    val placedSignature by viewModel.placedSignature.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Bug 3 fix: auto-colocar la firma recién dibujada ────────────────────
    //
    // Cuando el usuario termina de dibujar en DrawSignatureScreen y pulsa
    // "Listo", se guarda en SignatureRepository. savedSignatures.size aumenta.
    // Si en ese momento no hay ninguna firma colocada, la colocamos automáticamente.
    // Así el usuario vuelve a SignatureScreen y ya ve la firma sobre el documento
    // sin tener que abrir el sheet y seleccionarla manualmente.
    val prevSavedCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(savedSignatures.size) {
        if (savedSignatures.size > prevSavedCount.intValue && placedSignature == null) {
            savedSignatures.lastOrNull()?.let { viewModel.placeSignature(it) }
        }
        prevSavedCount.intValue = savedSignatures.size
    }

    // Lanzador para importar firma desde galería del dispositivo
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = context.contentResolver.openInputStream(it)
                ?.use { stream -> android.graphics.BitmapFactory.decodeStream(stream) }
            bitmap?.let { bmp -> viewModel.placeSignature(bmp) }
        }
    }

    SignatureScreenContent(
        documentBitmap      = images.lastOrNull(),
        savedSignatures     = savedSignatures,
        placedSignature     = placedSignature,
        onBack              = onBack,
        onDrawNew           = onDrawNew,
        onSignatureSelected = { viewModel.placeSignature(it) },
        onMoveSignature     = { dx, dy -> viewModel.moveSignature(dx, dy) },
        onResizeSignature   = { dw, dh -> viewModel.resizeSignature(dw, dh) },
        onUpdateColor       = { color -> viewModel.updateSignatureColor(color) },
        onRemovePlaced      = { viewModel.removePlacedSignature() },
        onImportFromGallery = { galleryLauncher.launch("image/*") },
        onConfirm           = { imageSize, density ->
            viewModel.applySignatureAtPosition(imageSize, density)
            onBack()
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SignatureScreenContent — composable puro (sin Koin), testeable con @Preview
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreenContent(
    documentBitmap: Bitmap?,
    savedSignatures: List<Bitmap>,
    placedSignature: PlacedSignature?,
    onBack: () -> Unit,
    onDrawNew: () -> Unit,
    onSignatureSelected: (Bitmap) -> Unit,
    onMoveSignature: (Float, Float) -> Unit,
    onResizeSignature: (Float, Float) -> Unit,   // dWidthDp, dHeightDp
    onUpdateColor: (Color) -> Unit,
    onRemovePlaced: () -> Unit,
    onImportFromGallery: () -> Unit,
    onConfirm: (IntSize, Float) -> Unit          // imageLayoutSize, screenDensity
) {
    // ── Estado local de la pantalla ──────────────────────────────────────────
    var showSignatureSheet  by remember { mutableStateOf(false) }
    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var showEditPanel       by remember { mutableStateOf(false) }

    // strokeWidth solo es visual (slider de edición), no afecta el bake
    var strokeWidth by remember { mutableFloatStateOf(0.4f) }

    // Tamaño en px del widget Image() — necesario para escalar posición al bitmap
    var imageLayoutSize by remember { mutableStateOf(IntSize.Zero) }

    // Densidad de pantalla (dp → px) — necesaria para applySignatureAtPosition
    val density = LocalDensity.current.density

    // ── rememberUpdatedState — callbacks siempre actualizados ───────────────
    //
    // pointerInput usa coroutines que se lanzan UNA vez (con clave Unit).
    // Si usáramos las lambdas directamente, el coroutine capturaría la versión
    // inicial y nunca vería actualizaciones.
    // rememberUpdatedState garantiza que la coroutine siempre llama la lambda
    // más reciente sin reiniciarse (lo que cancelaría el gesto en curso).
    val latestOnMove   by rememberUpdatedState(onMoveSignature)
    val latestOnResize by rememberUpdatedState(onResizeSignature)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firmar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                // [Firma y sello] — abre el sheet de firmas guardadas
                TextButton(
                    onClick = { showSignatureSheet = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Firma y sello")
                }

                // [Fecha] — placeholder futuro
                TextButton(
                    onClick = { /* próximamente */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Fecha")
                }

                // [✓] — aplica la firma y vuelve a Preview
                // Solo habilitado cuando hay una firma colocada
                Button(
                    onClick = { onConfirm(imageLayoutSize, density) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                    modifier = Modifier.padding(end = 8.dp),
                    enabled = placedSignature != null
                ) {
                    Text("✓", color = Color.White)
                }
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Documento de fondo ───────────────────────────────────────────
            if (documentBitmap != null) {
                Image(
                    bitmap = documentBitmap.asImageBitmap(),
                    contentDescription = "Documento",
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            // Captura el tamaño real del widget en pantalla
                            imageLayoutSize = coords.size
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // ── Overlay arrastrable de la firma ──────────────────────────────
            //
            // El overlay tiene 3 zonas de interacción:
            //   • Esquina superior-izquierda (+) : indicador visual, sin acción
            //   • Esquina superior-derecha  (...): abre/cierra panel "Editar"
            //   • Esquina inferior-derecha  (↗) : drag para redimensionar
            //   • Zona central (imagen)          : drag para mover
            //
            placedSignature?.let { placed ->

                Box(
                    modifier = Modifier
                        .offset {
                            // placed.offsetX / offsetY están en px de pantalla
                            IntOffset(
                                placed.offsetX.roundToInt(),
                                placed.offsetY.roundToInt()
                            )
                        }
                        // Tamaño controlado por el ViewModel (en dp)
                        .size(width = placed.widthDp.dp, height = placed.heightDp.dp)
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFF00BFA5),
                            shape = RoundedCornerShape(4.dp)
                        )
                        // ── Drag en el Box completo = mover overlay ──────────
                        //
                        // CLAVE: pointerInput(Unit) — la clave nunca cambia, así
                        // el coroutine de detectDragGestures vive toda la sesión
                        // sin cancelarse. Si usáramos pointerInput(placed), cada
                        // movimiento actualizaría `placed`, cambiaría la clave y
                        // cancela el gesto → la firma no se podría arrastrar.
                        //
                        // latestOnMove viene de rememberUpdatedState → siempre
                        // apunta a la lambda más reciente sin reiniciar la coroutine.
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                latestOnMove(dragAmount.x, dragAmount.y)
                            }
                        }
                ) {

                    // ── Imagen de la firma ────────────────────────────────────
                    //
                    // ColorFilter.tint aplica el color de tinta elegido.
                    // BlendMode.SrcIn: reemplaza los píxeles opacos de la firma
                    // con el color seleccionado, preservando transparencia.
                    Image(
                        bitmap = placed.bitmap.asImageBitmap(),
                        contentDescription = "Firma",
                        colorFilter = if (placed.tintColor != Color.Black) {
                            ColorFilter.tint(placed.tintColor)
                        } else null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),    // deja espacio visible para los controles de esquina
                        contentScale = ContentScale.Fit
                    )

                    // ── Esquina superior-izquierda: botón X (quitar firma) ───
                    //
                    // Cambiamos + por X para permitir quitar la firma fácilmente
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(Color(0xFF00BFA5))
                    ) {
                        IconButton(
                            onClick = onRemovePlaced,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar firma",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // ── Esquina superior-derecha: botón "..." (panel Editar) ─
                    //
                    // Al pulsarlo muestra/oculta el panel de edición de color.
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF00BFA5))
                    ) {
                        IconButton(
                            onClick = { showEditPanel = !showEditPanel },
                            modifier = Modifier.size(24.dp)
                        ) {
                            // Usamos "⋮" como texto porque Material no tiene MoreVert pequeño aquí
                            Text("⋮", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    // ── Esquina inferior-derecha: handle de redimensión ──────
                    //
                    // detectDragGestures convierte el dragAmount de px a dp
                    // dividiendo por la densidad de pantalla antes de enviarlo
                    // al ViewModel (que trabaja en dp para Modifier.size()).
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF00BFA5))
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    // px → dp para que ViewModel sea consistente
                                    latestOnResize(
                                        dragAmount.x / density,
                                        dragAmount.y / density
                                    )
                                }
                            }
                    ) {
                        // Icono de flecha diagonal para indicar redimensión
                        Text(
                            "↗",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // ── Panel "Editar" (desliza desde abajo) ─────────────────────────
            //
            // AnimatedVisibility muestra/oculta el panel con animación vertical.
            // slideInVertically: entra desde la parte inferior (initialOffsetY > 0).
            // slideOutVertically: sale hacia la parte inferior.
            AnimatedVisibility(
                visible = showEditPanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit  = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                EditSignaturePanel(
                    currentColor = placedSignature?.tintColor ?: Color.Black,
                    strokeWidth  = strokeWidth,
                    onColorSelected = { color ->
                        onUpdateColor(color)
                    },
                    onStrokeWidthChanged = { strokeWidth = it },
                    onClose = { showEditPanel = false }
                )
            }
        }

        // ── Bottom Sheets ─────────────────────────────────────────────────────

        // Sheet 1: firmas guardadas
        if (showSignatureSheet) {
            SignatureBottomSheet(
                savedSignatures = savedSignatures,
                onSignatureSelected = { bitmap ->
                    onSignatureSelected(bitmap)
                    showSignatureSheet = false
                },
                onAddNew = {
                    showSignatureSheet = false
                    showAddOptionsSheet = true
                },
                onDismiss = { showSignatureSheet = false }
            )
        }

        // Sheet 2: opciones añadir firma
        if (showAddOptionsSheet) {
            AddSignatureOptionsSheet(
                onCreateSignature = {
                    showAddOptionsSheet = false
                    onDrawNew()
                },
                onScanSignature = {
                    showAddOptionsSheet = false
                    // placeholder: próximamente
                },
                onImportFromGallery = {
                    showAddOptionsSheet = false
                    onImportFromGallery()
                },
                onDismiss = { showAddOptionsSheet = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EditSignaturePanel
//
// Panel que aparece deslizando desde abajo al pulsar "⋮" en el overlay.
// Permite cambiar el color de tinta y el grosor visual del trazo.
//
// Colores disponibles (igual que CamScanner):
//   Sin color (transparente/gris) | Negro | Azul | Rojo | Blanco | Marrón | Naranja
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EditSignaturePanel(
    currentColor: Color,
    strokeWidth: Float,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onClose: () -> Unit
) {
    // Paleta de colores disponibles para la firma
    val colors = listOf(
        Color.Gray,          // Sin color / deshabilitar tinte
        Color.Black,
        Color(0xFF1565C0),   // Azul oscuro
        Color(0xFFC62828),   // Rojo
        Color.White,
        Color(0xFF4E342E),   // Marrón
        Color(0xFFE65100)    // Naranja
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        // ── Encabezado del panel ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Editar",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onClose) {
                Text("OK", color = Color(0xFF00BFA5))
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Slider de grosor de trazo (solo visual) ───────────────────────────
        //
        // Este slider no afecta el bake en bitmap; es un indicador
        // de previsualización del grosor del trazado.
        Text(
            text = "Grosor",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = strokeWidth,
            onValueChange = onStrokeWidthChanged,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // ── Selector de color ────────────────────────────────────────────────
        //
        // Fila de círculos de color. El color activo tiene un borde teal.
        // El primero (gris) desactiva el tinte → la firma aparece en su
        // color original (negro por defecto del canvas DrawSignature).
        Text(
            text = "Color de tinta",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { color ->
                val isSelected = color == currentColor
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        // Borde teal cuando está seleccionado
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF00BFA5) else Color.LightGray,
                            shape = CircleShape
                        )
                        .background(color)
                        // clickable es la forma correcta de responder a taps en Compose
                        .clickable { onColorSelected(color) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun SignatureScreenEmptyPreview() {
    ConvertPngToPdfTheme {
        SignatureScreenContent(
            documentBitmap      = null,
            savedSignatures     = emptyList(),
            placedSignature     = null,
            onBack              = {},
            onDrawNew           = {},
            onSignatureSelected = {},
            onMoveSignature     = { _, _ -> },
            onResizeSignature   = { _, _ -> },
            onUpdateColor       = {},
            onRemovePlaced      = {},
            onImportFromGallery = {},
            onConfirm           = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditSignaturePanelPreview() {
    ConvertPngToPdfTheme {
        EditSignaturePanel(
            currentColor         = Color.Black,
            strokeWidth          = 0.4f,
            onColorSelected      = {},
            onStrokeWidthChanged = {},
            onClose              = {}
        )
    }
}
