package com.nirv.converttopdf.ui.imageedit

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
    onBack: () -> Unit,
    onDrawNew: () -> Unit,
    viewModel: ImageEditViewModel = koinViewModel(
        key        = "image_edit_$pageId",
        parameters = { parametersOf(pageId, imagePath) }
    )
) {
    val imagePath        = viewModel.imagePath
    val savedSignatures  by viewModel.savedSignatures.collectAsStateWithLifecycle()
    val placedSignatures by viewModel.placedSignatures.collectAsStateWithLifecycle()
    val placedTexts      by viewModel.placedTexts.collectAsStateWithLifecycle()
    val selectedSigId    by viewModel.selectedSignatureId.collectAsStateWithLifecycle()
    val selectedTextId   by viewModel.selectedTextId.collectAsStateWithLifecycle()
    val context          = LocalContext.current
    val density          = LocalDensity.current.density

    var showSignatureSheet  by remember { mutableStateOf(false) }
    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var showEditSigPanel    by remember { mutableStateOf(false) }
    var showTextDialog      by remember { mutableStateOf(false) }
    var showEditTextDialog  by remember { mutableStateOf<Int?>(null) }
    var strokeWidth         by remember { mutableFloatStateOf(0.4f) }
    var imageDisplaySize    by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "ImageEditScreen: pageId=$pageId, imagePath=$imagePath")
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
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
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
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline,
                    thickness = 0.5.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick  = { showSignatureSheet = true },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Firma")
                    }
                    TextButton(
                        onClick  = { showTextDialog = true },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Texto")
                    }
                    TextButton(
                        onClick  = { viewModel.placeDateText() },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Fecha")
                    }
                    Button(
                        onClick  = {
                            viewModel.confirmAndSave(imageDisplaySize, density) { onBack() }
                        },
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("✓", color = Color.White)
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
            Log.d(TAG, "Building Coil request for path=$imagePath, fileExists=${File(imagePath).exists()}")
            val request = ImageRequest.Builder(context)
                .data(File(imagePath))
                .memoryCacheKey(imagePath)
                .diskCacheKey(imagePath)
                .build()
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
