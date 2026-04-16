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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignatureScreen(
    onBack: () -> Unit,
    onDrawNew: () -> Unit,
    viewModel: SignatureViewModel = koinViewModel()
) {
    val images           by viewModel.images.collectAsStateWithLifecycle()
    val savedSignatures  by viewModel.savedSignatures.collectAsStateWithLifecycle()
    val placedSignatures by viewModel.placedSignatures.collectAsStateWithLifecycle()
    val selectedId       by viewModel.selectedId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    // Auto-coloca firmas nuevas (dibujadas desde esta pantalla) cerca del área visible
    LaunchedEffect(savedSignatures) {
        viewModel.checkAndAutoPlaceNewSignatures(scrollState.value)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = context.contentResolver.openInputStream(it)
                ?.use { stream -> android.graphics.BitmapFactory.decodeStream(stream) }
            bitmap?.let { bmp ->
                viewModel.placeSignature(bmp, initialY = scrollState.value.toFloat() + 120f)
            }
        }
    }

    SignatureScreenContent(
        images           = images,
        savedSignatures  = savedSignatures,
        placedSignatures = placedSignatures,
        selectedId       = selectedId,
        scrollState      = scrollState,
        onBack           = onBack,
        onDrawNew        = onDrawNew,
        onSignatureSelected = { bitmap ->
            viewModel.placeSignature(bitmap, initialY = scrollState.value.toFloat() + 120f)
        },
        onSelectSignature   = { viewModel.selectSignature(it) },
        onDeselectAll       = { viewModel.deselectAll() },
        onMoveSignature     = { id, dx, dy -> viewModel.moveSignature(id, dx, dy) },
        onResizeSignature   = { id, dw, dh -> viewModel.resizeSignature(id, dw, dh) },
        onRotateSignature   = { id, delta -> viewModel.rotateSignature(id, delta) },
        onUpdateColor       = { id, color -> viewModel.updateSignatureColor(id, color) },
        onRemovePlaced      = { viewModel.removePlacedSignature(it) },
        onImportFromGallery = { galleryLauncher.launch("image/*") },
        onConfirm           = { imageLayouts, density ->
            viewModel.applyAllSignatures(imageLayouts, density)
            onBack()
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SignatureScreenContent
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreenContent(
    images: List<Bitmap>,
    savedSignatures: List<Bitmap>,
    placedSignatures: List<PlacedSignature>,
    selectedId: Int?,
    scrollState: androidx.compose.foundation.ScrollState,
    onBack: () -> Unit,
    onDrawNew: () -> Unit,
    onSignatureSelected: (Bitmap) -> Unit,
    onSelectSignature: (Int?) -> Unit,
    onDeselectAll: () -> Unit,
    onMoveSignature: (Int, Float, Float) -> Unit,
    onResizeSignature: (Int, Float, Float) -> Unit,
    onRotateSignature: (Int, Float) -> Unit,
    onUpdateColor: (Int, Color) -> Unit,
    onRemovePlaced: (Int) -> Unit,
    onImportFromGallery: () -> Unit,
    onConfirm: (Map<Int, ImageLayoutInfo>, Float) -> Unit
) {
    var showSignatureSheet  by remember { mutableStateOf(false) }
    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var showEditPanel       by remember { mutableStateOf(false) }
    var strokeWidth         by remember { mutableFloatStateOf(0.4f) }
    val density             = LocalDensity.current.density

    // Layout de cada imagen: clave = índice, valor = topY en canvas + tamaño display
    val imageLayouts = remember { mutableStateMapOf<Int, ImageLayoutInfo>() }

    LaunchedEffect(selectedId) {
        if (selectedId == null) showEditPanel = false
    }

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
                TextButton(
                    onClick  = { showSignatureSheet = true },
                    modifier = Modifier.weight(1f),
                    colors   = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Firma y sello")
                }
                TextButton(
                    onClick  = { /* próximamente */ },
                    modifier = Modifier.weight(1f),
                    colors   = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Fecha")
                }
                Button(
                    onClick  = { onConfirm(imageLayouts.toMap(), density) },
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                    modifier = Modifier.padding(end = 8.dp)
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
            // ── Canvas scrollable: imágenes + firmas en el mismo espacio ──────
            //
            // Ambas capas (imágenes y overlays de firma) viven dentro del mismo Box
            // con verticalScroll, por lo que comparten el mismo sistema de coordenadas
            // y desplazan juntas al hacer scroll.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    // Tap en área vacía → deselecciona la firma activa
                    .pointerInput(Unit) { detectTapGestures { onDeselectAll() } }
            ) {
                // ── Columna de imágenes ──────────────────────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    images.forEachIndexed { index, bitmap ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .onGloballyPositioned { coords ->
                                    // positionInParent() = y desde el tope del Column (= canvas)
                                    imageLayouts[index] = ImageLayoutInfo(
                                        topY        = coords.positionInParent().y,
                                        displaySize = coords.size
                                    )
                                }
                        ) {
                            Image(
                                bitmap             = bitmap.asImageBitmap(),
                                contentDescription = "Página ${index + 1}",
                                modifier           = Modifier.fillMaxWidth(),
                                contentScale       = ContentScale.FillWidth
                            )
                            // Badge de página
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x99000000))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .align(Alignment.TopStart)
                            ) {
                                Text(
                                    text     = "${index + 1}/${images.size}",
                                    color    = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    // Espacio al final para poder mover firmas hasta el fondo de la última imagen
                    Spacer(Modifier.height(80.dp))
                }

                // ── Firma(s) superpuestas sobre el canvas ────────────────────
                //
                // Se posicionan con offset absoluto dentro del mismo Box scrollable,
                // así se desplazan junto con las imágenes al hacer scroll.
                placedSignatures.forEach { placed ->
                    SignatureOverlay(
                        placed     = placed,
                        isSelected = placed.id == selectedId,
                        density    = density,
                        onSelect   = { onSelectSignature(placed.id) },
                        onMove     = { dx, dy -> onMoveSignature(placed.id, dx, dy) },
                        onResize   = { dw, dh -> onResizeSignature(placed.id, dw, dh) },
                        onRotate   = { delta -> onRotateSignature(placed.id, delta) },
                        onRemove   = { onRemovePlaced(placed.id) },
                        onOpenEdit = { showEditPanel = true }
                    )
                }
            }

            // ── Panel "Editar" (desliza desde abajo, fuera del scroll) ───────
            val selectedSignature = placedSignatures.firstOrNull { it.id == selectedId }
            AnimatedVisibility(
                visible  = showEditPanel && selectedSignature != null,
                enter    = slideInVertically(initialOffsetY = { it }),
                exit     = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (selectedSignature != null) {
                    EditSignaturePanel(
                        currentColor         = selectedSignature.tintColor,
                        strokeWidth          = strokeWidth,
                        onColorSelected      = { color -> onUpdateColor(selectedSignature.id, color) },
                        onStrokeWidthChanged = { strokeWidth = it },
                        onClose              = { showEditPanel = false }
                    )
                }
            }
        }

        // ── Bottom Sheets ─────────────────────────────────────────────────────

        if (showSignatureSheet) {
            SignatureBottomSheet(
                savedSignatures     = savedSignatures,
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

        if (showAddOptionsSheet) {
            AddSignatureOptionsSheet(
                onCreateSignature = {
                    showAddOptionsSheet = false
                    onDrawNew()
                },
                onScanSignature     = { showAddOptionsSheet = false },
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
// SignatureOverlay — firma flotante en el canvas
//
//   isSelected = false → solo imagen, tap para seleccionar
//   isSelected = true  → borde teal + controles X / ⋮ / ↗, drag para mover
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SignatureOverlay(
    placed: PlacedSignature,
    isSelected: Boolean,
    density: Float,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onRotate: (Float) -> Unit,
    onRemove: () -> Unit,
    onOpenEdit: () -> Unit
) {
    val latestOnMove   by rememberUpdatedState(onMove)
    val latestOnResize by rememberUpdatedState(onResize)
    val latestOnRotate by rememberUpdatedState(onRotate)

    Box(
        modifier = Modifier
            .offset { IntOffset(placed.offsetX.roundToInt(), placed.offsetY.roundToInt()) }
            .size(width = placed.widthDp.dp, height = placed.heightDp.dp)
            // Aplica rotación visual alrededor del centro del overlay
            .graphicsLayer { rotationZ = placed.rotation }
            .then(
                if (isSelected) Modifier.border(
                    width = 1.5.dp,
                    color = Color(0xFF00BFA5),
                    shape = RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .then(
                if (isSelected) Modifier.pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        latestOnMove(dragAmount.x, dragAmount.y)
                    }
                } else Modifier
            )
    ) {
        // Imagen de la firma
        Image(
            bitmap       = placed.bitmap.asImageBitmap(),
            contentDescription = "Firma",
            colorFilter  = if (placed.tintColor != Color.Black) {
                ColorFilter.tint(placed.tintColor)
            } else null,
            modifier     = Modifier
                .fillMaxSize()
                .padding(if (isSelected) 20.dp else 0.dp)
                .then(
                    if (!isSelected) Modifier.pointerInput(Unit) {
                        detectTapGestures { onSelect() }
                    } else Modifier
                ),
            contentScale = ContentScale.Fit
        )

        if (isSelected) {
            // X — eliminar (esquina superior-izquierda)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Color(0xFF00BFA5))
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Quitar firma",
                        tint     = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // ⋮ — editar color (esquina superior-derecha)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF00BFA5))
            ) {
                IconButton(onClick = onOpenEdit, modifier = Modifier.size(24.dp)) {
                    Text("⋮", color = Color.White, fontSize = 14.sp)
                }
            }

            // ⤡ — escalar proporcionalmente (esquina inferior-derecha)
            //   drag hacia abajo/derecha → agranda
            //   drag hacia arriba/izquierda → encoge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF00BFA5))
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val scaleDp = (dragAmount.x + dragAmount.y) / 2f / density
                            latestOnResize(scaleDp, scaleDp)
                        }
                    }
            ) {
                Text(
                    "⤡",
                    color    = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EditSignaturePanel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EditSignaturePanel(
    currentColor: Color,
    strokeWidth: Float,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onClose: () -> Unit
) {
    val colors = listOf(
        Color.Gray, Color.Black,
        Color(0xFF1565C0), Color(0xFFC62828),
        Color.White, Color(0xFF4E342E), Color(0xFFE65100)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Editar", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClose) { Text("OK", color = Color(0xFF00BFA5)) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Grosor", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = strokeWidth, onValueChange = onStrokeWidthChanged,
            valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("Color de tinta", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            colors.forEach { color ->
                val sel = color == currentColor
                Box(
                    modifier = Modifier
                        .size(32.dp).clip(CircleShape)
                        .border(if (sel) 3.dp else 1.dp,
                            if (sel) Color(0xFF00BFA5) else Color.LightGray, CircleShape)
                        .background(color).clickable { onColorSelected(color) }
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
            images              = emptyList(),
            savedSignatures     = emptyList(),
            placedSignatures    = emptyList(),
            selectedId          = null,
            scrollState         = rememberScrollState(),
            onBack              = {},
            onDrawNew           = {},
            onSignatureSelected = {},
            onSelectSignature   = {},
            onDeselectAll       = {},
            onMoveSignature     = { _, _, _ -> },
            onResizeSignature   = { _, _, _ -> },
            onRotateSignature   = { _, _ -> },
            onUpdateColor       = { _, _ -> },
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
            currentColor = Color.Black, strokeWidth = 0.4f,
            onColorSelected = {}, onStrokeWidthChanged = {}, onClose = {}
        )
    }
}
