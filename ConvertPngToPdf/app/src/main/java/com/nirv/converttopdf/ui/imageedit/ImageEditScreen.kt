package com.nirv.converttopdf.ui.imageedit

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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

private const val TAG = "ImageEditScreen"

@Composable
fun ImageEditScreen(
    pageId: Long,
    imagePath: String,
    allPagesFlow: kotlinx.coroutines.flow.StateFlow<List<com.nirv.converttopdf.data.db.entity.DocumentPageEntity>>? = null,
    onBack: () -> Unit,
    onDrawNew: () -> Unit,
    onPageNavigate: ((pageId: Long, imagePath: String) -> Unit)? = null,
    onToolSelected: (tool: String) -> Unit = {},
    viewModel: ImageEditViewModel = koinViewModel(
        key        = "image_edit_$pageId",
        parameters = { parametersOf(pageId, imagePath, allPagesFlow?.value ?: emptyList<com.nirv.converttopdf.data.db.entity.DocumentPageEntity>()) }
    )
) {
    val currentImagePath by viewModel.currentImagePath.collectAsStateWithLifecycle()
    val savedSignatures  by viewModel.savedSignatures.collectAsStateWithLifecycle()
    val placedSignatures by viewModel.placedSignatures.collectAsStateWithLifecycle()
    val placedTexts      by viewModel.placedTexts.collectAsStateWithLifecycle()
    val selectedSigId    by viewModel.selectedSignatureId.collectAsStateWithLifecycle()
    val selectedTextId   by viewModel.selectedTextId.collectAsStateWithLifecycle()
    val currentIndex     by viewModel.currentIndex.collectAsStateWithLifecycle()
    val imageVersion     by viewModel.imageVersion.collectAsStateWithLifecycle()
    val context          = LocalContext.current
    val density          = LocalDensity.current.density

    val allPages by (allPagesFlow ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())

    // para que son estas variables
    var showSignatureSheet  by remember { mutableStateOf(false) }
    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var showEditSigPanel    by remember { mutableStateOf(false) }
    var showTextDialog      by remember { mutableStateOf(false) }
    var showEditTextDialog  by remember { mutableStateOf<Int?>(null) }
    var strokeWidth         by remember { mutableFloatStateOf(0.4f) }
    var imageDisplaySize    by remember { mutableStateOf(IntSize.Zero) }
    var carouselState = rememberLazyListState()
    var activeTool    by remember { mutableStateOf<String?>(null) }

    // Efecto
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < allPages.size) {
            carouselState.animateScrollToItem(currentIndex)
        }
    }

    LaunchedEffect(currentImagePath) {
        Log.d(TAG, "ImageEditScreen: currentImagePath=$currentImagePath")
    }

    LaunchedEffect(savedSignatures) {
        viewModel.checkAndAutoPlaceNewSignature()
    }

    LaunchedEffect(selectedSigId) {
        if (selectedSigId == null) showEditSigPanel = false
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

    // Text input dialog — add new text
    if (showTextDialog) {
        var textInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTextDialog = false; textInput = "" },
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
                TextButton(onClick = { showTextDialog = false; textInput = "" }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.placeText(textInput.trim())
                    showTextDialog = false
                    textInput = ""
                }) { Text("Agregar") }
            }
        )
    }

    // Text edit dialog — modify existing text
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
                    .background(MaterialTheme.colorScheme.surface)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver",
                            modifier = Modifier.size(22.dp))
                    }
                    Text(
                        text = "Editar página",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                }
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF1A1C1E))
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Carrusel oculto mientras hay una herramienta activa
                if (allPages.isNotEmpty() && activeTool == null) {
                    LazyRow(
                        state                 = carouselState,
                        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(allPages) { index, page ->
                            val isSelected = index == currentIndex
                            Box(
                                modifier = Modifier
                                    .width(65.dp)
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF5D71F4) else Color(0xFF3E4146),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { if (!isSelected) viewModel.goToPage(index) }
                            ) {
                                SubcomposeAsyncImage(
                                    model              = File(page.imagePath),
                                    contentDescription = null,
                                    modifier           = Modifier.fillMaxSize(),
                                    contentScale       = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(if (isSelected) Color(0xFF5D71F4) else Color(0xCC000000))
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text      = "${index + 1}",
                                        color     = Color.White,
                                        fontSize  = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier  = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Panel de herramienta activa (inline, sin modal)
                AnimatedVisibility(visible = activeTool != null) {
                    when (activeTool) {
                        "crop" -> CropActionsPanel(
                            onRotateLeft  = { viewModel.rotateImage(-90f) },
                            onRotateRight = { viewModel.rotateImage(90f) },
                            onFlipH       = { viewModel.flipImage(true) },
                            onFlipV       = { viewModel.flipImage(false) }
                        )
                        "filter" -> FilterPanel(
                            currentImagePath = currentImagePath,
                            imageVersion     = imageVersion,
                            onApply          = { filter -> viewModel.applyFilterAndSave(filter); activeTool = null },
                            onDismiss        = { activeTool = null }
                        )
                        "signature" -> SignatureToolPanel(
                            onAddSignature = { showSignatureSheet = true }
                        )
                        "text" -> TextToolPanel(
                            onAddText = { showTextDialog = true },
                            onAddDate = { viewModel.placeDateText() }
                        )
                    }
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 0.5.dp)

                // Barra de herramientas principal
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
                        EditToolTab(Icons.Default.Crop,       "Cultivos",  activeTool == "crop")      { activeTool = if (activeTool == "crop")      null else "crop" }
                        EditToolTab(Icons.Default.Gesture,    "Garabato",  activeTool == "signature") { activeTool = if (activeTool == "signature") null else "signature" }
                        EditToolTab(Icons.Default.ColorLens,  "Filtro",    activeTool == "filter")    { activeTool = if (activeTool == "filter")    null else "filter" }
                        EditToolTab(Icons.Default.TextFields, "Texto",     activeTool == "text")      { activeTool = if (activeTool == "text")      null else "text" }
                    }
                    IconButton(
                        onClick = {
                            if (activeTool != null) activeTool = null
                            else viewModel.confirmAndSave(imageDisplaySize, density) { onBack() }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5D71F4))
                    ) {
                        Icon(Icons.Default.Check, "Confirmar", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) { detectTapGestures { viewModel.deselectAll() } }
        ) {
            AnimatedContent(
                targetState = currentImagePath,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "page_transition"
            ) { imagePath ->
                val cacheKey = "$imagePath?v=$imageVersion"
                val request = remember(cacheKey) {
                    ImageRequest.Builder(context)
                        .data(File(imagePath))
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .build()
                }
                val centerImage = activeTool == "crop" || activeTool == "filter"
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = if (centerImage) Alignment.Center else Alignment.TopStart
                ) {
                SubcomposeAsyncImage(
                    model              = request,
                    contentDescription = "Página",
                    modifier           = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            if (imageDisplaySize != coords.size) {
                                imageDisplaySize = coords.size
                                Log.d(TAG, "imageDisplaySize updated → ${coords.size}")
                            }
                        },
                    contentScale = ContentScale.FillWidth,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    },
                    error = {
                        Log.e(TAG, "Coil error loading: $imagePath")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se pudo cargar la imagen", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                } // Box centrador
            }

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

            placedTexts.forEach { placed ->
                TextOverlay(
                    placed     = placed,
                    isSelected = placed.id == selectedTextId,
                    onSelect   = { viewModel.selectText(placed.id) },
                    onMove     = { dx, dy -> viewModel.moveText(placed.id, dx, dy) },
                    onRemove   = { viewModel.removePlacedText(placed.id) },
                    onEdit     = { showEditTextDialog = placed.id }
                )
            }

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
                    galleryLauncher.launch("image/*")
                },
                onDismiss = { showAddOptionsSheet = false }
            )
        }
    }
}

@Composable
private fun EditToolTab(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val color = if (isActive) Color(0xFFFFBD00) else Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun SignatureToolPanel(onAddSignature: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1C1E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2D2F33))
                .clickable { onAddSignature() }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text("Agregar firma", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TextToolPanel(onAddText: () -> Unit, onAddDate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1C1E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2D2F33))
                    .clickable { onAddText() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("Agregar texto", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2D2F33))
                    .clickable { onAddDate() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("Fecha", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TextOverlay(
    placed: PlacedText,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(placed.offsetX.roundToInt(), placed.offsetY.roundToInt()) }
            .wrapContentSize()
            .then(
                if (isSelected) Modifier.border(
                    1.5.dp, Color(0xFF00BFA5), RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .then(
                if (isSelected) Modifier.pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        onMove(dragAmount.x, dragAmount.y)
                    }
                } else Modifier
            )
    ) {
        Text(
            text     = placed.text,
            fontSize = placed.fontSize.sp,
            color    = placed.color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
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
                    Icon(Icons.Default.Close, "Quitar texto",
                        tint = Color.White, modifier = Modifier.size(14.dp))
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
                    Icon(Icons.Default.Edit, "Editar texto",
                        tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
internal fun CropActionsPanel(
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1C1E))
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        CropActionItem(Icons.AutoMirrored.Filled.RotateLeft, "Rot. izq.") { onRotateLeft() }
        CropActionItem(Icons.AutoMirrored.Filled.RotateRight, "Rot. der.") { onRotateRight() }
        CropActionItem(Icons.Default.Flip, "Voltear H") { onFlipH() }
        CropActionItem(
            icon    = Icons.Default.Flip,
            label   = "Voltear V",
            rotated = true,
            onClick = onFlipV
        )
    }
}

@Composable
internal fun CropActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    rotated: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier
                .size(26.dp)
                .then(if (rotated) Modifier.graphicsLayer { rotationZ = 90f } else Modifier)
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color(0xFFBBBBBB), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun FilterPanel(
    currentImagePath: String,
    imageVersion: Long,
    onApply: (ImageFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(ImageFilter.ORIGINAL) }
    var activeTab      by remember { mutableStateOf(0) }
    val filters        = ImageFilter.entries

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1C1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Cancelar", tint = Color.White)
            }
            TabRow(
                selectedTabIndex = activeTab,
                modifier         = Modifier.weight(1f),
                containerColor   = Color.Transparent,
                contentColor     = Color.White,
                indicator        = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color    = Color(0xFF5D71F4)
                    )
                }
            ) {
                listOf("Filtro", "Ajustar").forEachIndexed { i, title ->
                    Tab(
                        selected = activeTab == i,
                        onClick  = { activeTab = i }
                    ) {
                        Text(
                            title,
                            modifier   = Modifier.padding(vertical = 10.dp),
                            fontWeight = if (activeTab == i) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            IconButton(onClick = { onApply(selectedFilter) }) {
                Icon(Icons.Default.Check, "Aplicar", tint = Color(0xFF5D71F4))
            }
        }

        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.4f), thickness = 0.5.dp)

        val cacheKey = "$currentImagePath?v=$imageVersion"
        LazyRow(
            contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier            = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(filters) { _, filter ->
                val isSelected = filter == selectedFilter
                val request = remember(cacheKey) {
                    ImageRequest.Builder(context)
                        .data(File(currentImagePath))
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .size(150)
                        .build()
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedFilter = filter }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF5D71F4) else Color(0xFF3E4146),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        SubcomposeAsyncImage(
                            model              = request,
                            contentDescription = filter.label,
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop,
                            colorFilter        = filter.toComposeColorFilter()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF5D71F4).copy(alpha = 0.15f))
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text       = filter.label,
                        color      = if (isSelected) Color(0xFF5D71F4) else Color(0xFF9E9E9E),
                        fontSize   = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
