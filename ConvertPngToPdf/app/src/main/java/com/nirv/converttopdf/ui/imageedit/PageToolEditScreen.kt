package com.nirv.converttopdf.ui.imageedit

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.nirv.converttopdf.ui.signature.AddSignatureOptionsSheet
import com.nirv.converttopdf.ui.signature.EditSignaturePanel
import com.nirv.converttopdf.ui.signature.SignatureBottomSheet
import com.nirv.converttopdf.ui.signature.SignatureOverlay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import kotlin.math.roundToInt

private const val PTAG = "PageToolEdit"

@Composable
fun PageToolEditScreen(
    pageId: Long,
    imagePath: String,
    initialTool: String = "",
    onBack: () -> Unit,
    onDrawNew: () -> Unit,
    viewModel: ImageEditViewModel = koinViewModel(
        key        = "image_edit_$pageId",
        parameters = { parametersOf(pageId, imagePath, emptyList<com.nirv.converttopdf.data.db.entity.DocumentPageEntity>()) }
    )
) {
    val currentImagePath by viewModel.currentImagePath.collectAsStateWithLifecycle()
    val imageVersion     by viewModel.imageVersion.collectAsStateWithLifecycle()
    val savedSignatures  by viewModel.savedSignatures.collectAsStateWithLifecycle()
    val placedSignatures by viewModel.placedSignatures.collectAsStateWithLifecycle()
    val placedTexts      by viewModel.placedTexts.collectAsStateWithLifecycle()
    val selectedSigId    by viewModel.selectedSignatureId.collectAsStateWithLifecycle()
    val selectedTextId   by viewModel.selectedTextId.collectAsStateWithLifecycle()
    val context          = LocalContext.current
    val density          = LocalDensity.current.density

    var activeTool         by remember { mutableStateOf(initialTool) }
    var showSignatureSheet  by remember { mutableStateOf(activeTool == "signature") }
    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var showEditSigPanel    by remember { mutableStateOf(false) }
    var showTextDialog      by remember { mutableStateOf(activeTool == "text") }
    var showEditTextDialog  by remember { mutableStateOf<Int?>(null) }
    var strokeWidth         by remember { mutableFloatStateOf(0.4f) }
    var imageDisplaySize    by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(selectedSigId) {
        if (selectedSigId == null) showEditSigPanel = false
    }

    LaunchedEffect(savedSignatures) {
        viewModel.checkAndAutoPlaceNewSignature()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bmp = context.contentResolver.openInputStream(it)
                ?.use { stream -> android.graphics.BitmapFactory.decodeStream(stream) }
            bmp?.let { b -> viewModel.placeSignature(b) }
        }
    }

    // Diálogo: agregar texto
    if (showTextDialog) {
        var textInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTextDialog = false; activeTool = "" },
            title   = { Text("Agregar texto", fontWeight = FontWeight.Bold) },
            text    = {
                OutlinedTextField(
                    value         = textInput,
                    onValueChange = { textInput = it },
                    singleLine    = true,
                    placeholder   = { Text("Escribe aquí…") },
                    shape         = RoundedCornerShape(12.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false; activeTool = "" }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.placeText(textInput.trim())
                    showTextDialog = false
                    activeTool = ""
                }) { Text("Agregar") }
            }
        )
    }

    // Diálogo: editar texto existente
    showEditTextDialog?.let { editId ->
        val placed = placedTexts.firstOrNull { it.id == editId }
        if (placed != null) {
            var editInput by remember(editId) { mutableStateOf(placed.text) }
            AlertDialog(
                onDismissRequest = { showEditTextDialog = null },
                title   = { Text("Editar texto", fontWeight = FontWeight.Bold) },
                text    = {
                    OutlinedTextField(
                        value         = editInput,
                        onValueChange = { editInput = it },
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp)
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showEditTextDialog = null }) { Text("Cancelar") }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateText(editId, newText = editInput.trim())
                        showEditTextDialog = null
                    }) { Text("Guardar") }
                }
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF1A1C1E))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Editar imagen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                    TextButton(onClick = { viewModel.resetToOriginal() }) {
                        Text("Reiniciar", color = Color(0xFF9E9E9E))
                    }
                }
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF1A1C1E))
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Panel de herramienta activa
                AnimatedVisibility(
                    visible = activeTool == "crop",
                    enter   = slideInVertically(initialOffsetY = { it }),
                    exit    = slideOutVertically(targetOffsetY = { it })
                ) {
                    CropActionsPanel(
                        onRotateLeft  = { viewModel.rotateImage(-90f) },
                        onRotateRight = { viewModel.rotateImage(90f) },
                        onFlipH       = { viewModel.flipImage(horizontal = true) },
                        onFlipV       = { viewModel.flipImage(horizontal = false) },
                        onClose       = { activeTool = "" }
                    )
                }

                AnimatedVisibility(
                    visible = activeTool == "filter",
                    enter   = slideInVertically(initialOffsetY = { it }),
                    exit    = slideOutVertically(targetOffsetY = { it })
                ) {
                    FilterPanel(
                        currentImagePath = currentImagePath,
                        imageVersion     = imageVersion,
                        onApply   = { filter ->
                            viewModel.applyFilterAndSave(filter)
                            activeTool = ""
                        },
                        onDismiss = { activeTool = "" }
                    )
                }

                // Barra de herramientas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 16.dp, bottom = 12.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        modifier              = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ToolButton(
                            icon     = Icons.Default.Crop,
                            label    = "Cultivos",
                            isActive = activeTool == "crop",
                            onClick  = { activeTool = if (activeTool == "crop") "" else "crop" }
                        )
                        ToolButton(
                            icon     = Icons.Default.Gesture,
                            label    = "Garabato",
                            isActive = activeTool == "signature",
                            onClick  = {
                                activeTool = "signature"
                                showSignatureSheet = true
                            }
                        )
                        ToolButton(
                            icon     = Icons.Default.ColorLens,
                            label    = "Filtro",
                            isActive = activeTool == "filter",
                            onClick  = { activeTool = if (activeTool == "filter") "" else "filter" }
                        )
                        ToolButton(
                            icon     = Icons.Default.TextFields,
                            label    = "Texto",
                            isActive = activeTool == "text",
                            onClick  = {
                                activeTool = "text"
                                showTextDialog = true
                            }
                        )
                    }

                    IconButton(
                        onClick  = { viewModel.confirmAndSave(imageDisplaySize, density) { onBack() } },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5D71F4))
                    ) {
                        Icon(Icons.Default.Check, "Guardar", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .pointerInput(Unit) { detectTapGestures { viewModel.deselectAll() } }
        ) {
            // Imagen a pantalla completa (ContentScale.Fit para ver todo)
            AnimatedContent(
                targetState  = currentImagePath,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label        = "tool_image_transition"
            ) { imgPath ->
                val cacheKey = "$imgPath?v=$imageVersion"
                val request = remember(cacheKey) {
                    ImageRequest.Builder(context)
                        .data(File(imgPath))
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .build()
                }
                SubcomposeAsyncImage(
                    model              = request,
                    contentDescription = "Página",
                    modifier           = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            if (imageDisplaySize != coords.size) {
                                imageDisplaySize = coords.size
                                Log.d(PTAG, "imageDisplaySize → ${coords.size}")
                            }
                        },
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2A2A2A))
                        )
                    }
                )
            }

            // Overlays de firma
            placedSignatures.forEach { placed ->
                SignatureOverlay(
                    placed     = placed,
                    isSelected = placed.id == selectedSigId,
                    density    = density,
                    onSelect   = { viewModel.selectSignature(placed.id) },
                    onMove     = { dx, dy -> viewModel.moveSignature(placed.id, dx, dy) },
                    onResize   = { dw, dh -> viewModel.resizeSignature(placed.id, dw, dh) },
                    onRotate   = { delta -> viewModel.rotateSignature(placed.id, delta) },
                    onRemove   = { viewModel.removePlacedSignature(placed.id) },
                    onOpenEdit = { showEditSigPanel = true }
                )
            }

            // Overlays de texto
            placedTexts.forEach { placed ->
                ToolTextOverlay(
                    placed     = placed,
                    isSelected = placed.id == selectedTextId,
                    onSelect   = { viewModel.selectText(placed.id) },
                    onMove     = { dx, dy -> viewModel.moveText(placed.id, dx, dy) },
                    onRemove   = { viewModel.removePlacedText(placed.id) },
                    onEdit     = { showEditTextDialog = placed.id }
                )
            }

            // Panel edición de firma seleccionada
            val selectedSig = placedSignatures.firstOrNull { it.id == selectedSigId }
            AnimatedVisibility(
                visible  = showEditSigPanel && selectedSig != null,
                enter    = slideInVertically(initialOffsetY = { it }),
                exit     = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (selectedSig != null) {
                    EditSignaturePanel(
                        currentColor         = selectedSig.tintColor,
                        strokeWidth          = strokeWidth,
                        onColorSelected      = { color -> viewModel.updateSignatureColor(selectedSig.id, color) },
                        onStrokeWidthChanged = { strokeWidth = it },
                        onClose              = { showEditSigPanel = false }
                    )
                }
            }
        }

        if (showSignatureSheet) {
            SignatureBottomSheet(
                savedSignatures     = savedSignatures,
                onSignatureSelected = { bmp ->
                    viewModel.placeSignature(bmp)
                    showSignatureSheet = false
                    activeTool = ""
                },
                onAddNew = {
                    showSignatureSheet = false
                    showAddOptionsSheet = true
                },
                onDismiss = {
                    showSignatureSheet = false
                    activeTool = ""
                }
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
                    galleryLauncher.launch("image/*")
                },
                onDismiss = { showAddOptionsSheet = false }
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFFFFC107)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint     = if (isActive) activeColor else Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color      = if (isActive) activeColor else Color.White,
            fontSize   = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ToolTextOverlay(
    placed: PlacedText,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .then(
                if (isSelected) Modifier.border(
                    1.5.dp, Color(0xFF00BFA5), RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .then(
                if (isSelected) Modifier.pointerInput(Unit) {
                    detectDragGestures { _, drag -> onMove(drag.x, drag.y) }
                } else Modifier
            )
    ) {
        Text(
            text       = placed.text,
            fontSize   = placed.fontSize.sp,
            color      = placed.color,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier
                .padding(if (isSelected) 20.dp else 8.dp)
                .then(
                    if (!isSelected) Modifier.pointerInput(Unit) {
                        detectTapGestures { onSelect() }
                    } else Modifier
                )
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Color(0xFF00BFA5))
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Quitar", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF00BFA5))
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, "Editar", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
