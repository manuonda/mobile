package com.nirv.converttopdf.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import com.nirv.converttopdf.ui.SplashScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.nirv.converttopdf.ui.capture.CaptureScreen
import com.nirv.converttopdf.ui.export.ExportScreen
import com.nirv.converttopdf.ui.files.DirectryFileScreen
import com.nirv.converttopdf.ui.home.HomeScreen
import com.nirv.converttopdf.ui.preview.PreviewScreen
import com.nirv.converttopdf.ui.settings.SettingsScreen
import com.nirv.converttopdf.ui.imageedit.ImageEditScreen
import com.nirv.converttopdf.ui.preview.PreviewViewModel
import com.nirv.converttopdf.ui.signature.DrawSignatureScreen
import com.nirv.converttopdf.ui.signature.SignatureScreen
import com.nirv.converttopdf.ui.theme.PlazoMuted
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// Tabs that show the bottom navigation bar
private fun isMainTab(dest: Any?): Boolean =
    dest is Home || dest is DirectoryFiles || dest is Settings

private val TRANSITION_MS = 320

// Transición estándar: desliza desde la derecha con fade
private val pushTransition: ContentTransform
    get() = (slideInHorizontally(tween(TRANSITION_MS)) { it / 2 } + fadeIn(tween(TRANSITION_MS))) togetherWith
            (slideOutHorizontally(tween(TRANSITION_MS)) { -it / 6 } + fadeOut(tween(TRANSITION_MS / 2)))

// Regreso: desliza hacia la derecha con fade
private val popTransition: ContentTransform
    get() = (slideInHorizontally(tween(TRANSITION_MS)) { -it / 6 } + fadeIn(tween(TRANSITION_MS))) togetherWith
            (slideOutHorizontally(tween(TRANSITION_MS)) { it / 2 } + fadeOut(tween(TRANSITION_MS / 2)))

// Transición para ImageEdit: sube desde abajo con fade (evita el "salto" de colores)
private val sheetEnterTransition: ContentTransform
    get() = (slideInVertically(tween(TRANSITION_MS)) { it / 3 } + fadeIn(tween(TRANSITION_MS))) togetherWith
            fadeOut(tween(TRANSITION_MS / 2))

private val sheetExitTransition: ContentTransform
    get() = fadeIn(tween(TRANSITION_MS / 2)) togetherWith
            (slideOutVertically(tween(TRANSITION_MS)) { it / 3 } + fadeOut(tween(TRANSITION_MS)))

@Composable
fun AppNavHost() {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val backStack = remember { mutableStateListOf<Any>(Home) }
    val currentDest = backStack.lastOrNull()

    Scaffold(
        contentWindowInsets = WindowInsets(0),   // evita doble inset con los Scaffolds internos
        bottomBar = {
            AnimatedVisibility(
                visible = isMainTab(currentDest),
                enter   = slideInVertically { it },
                exit    = slideOutVertically { it }
            ) {
                PlazoBottomNav(
                    currentDest = currentDest,
                    onHome = {
                        if (currentDest !is Home) {
                            backStack.clear()
                            backStack.add(Home)
                        }
                    },
                    onFiles = {
                        if (currentDest !is DirectoryFiles) {
                            backStack.clear()
                            backStack.add(Home)
                            backStack.add(DirectoryFiles)
                        }
                    },
                    onSettings = {
                        if (currentDest !is Settings) {
                            backStack.clear()
                            backStack.add(Home)
                            backStack.add(Settings)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(
            PaddingValues(
                top    = paddingValues.calculateTopPadding(),
                bottom = if (isMainTab(currentDest)) paddingValues.calculateBottomPadding() else 0.dp
            )
        )) {
            NavDisplay(
                backStack          = backStack,
                onBack             = { backStack.removeLastOrNull() },
                transitionSpec     = {
                    if (currentDest is ImageEdit || currentDest is PageToolEdit) sheetEnterTransition
                    else pushTransition
                },
                popTransitionSpec  = {
                    if (currentDest is ImageEdit || currentDest is PageToolEdit) sheetExitTransition
                    else popTransition
                },
                entryProvider = { key ->
                    when (key) {
                        is Home -> NavEntry(key = Home) {
                            HomeScreen(
                                onScanNew  = { backStack.add(Capture()) },
                                onFiles    = {
                                    backStack.clear()
                                    backStack.add(Home)
                                    backStack.add(DirectoryFiles)
                                },
                                onSettings = {
                                    backStack.clear()
                                    backStack.add(Home)
                                    backStack.add(Settings)
                                },
                                onDraftClick = { docId ->
                                    backStack.add(Preview(docId))
                                },
                            )
                        }

                        is Capture -> NavEntry(key = key) {
                            CaptureScreen(
                                autoLaunchScanner = key.autoLaunchScanner,
                                documentId        = key.documentId,
                                onBack            = { backStack.removeLastOrNull() },
                                onDocumentReady   = { docId ->
                                    // Remove this Capture entry
                                    backStack.removeLastOrNull()
                                    // If we were adding to an existing doc, Preview is already in the stack
                                    if (backStack.none { it is Preview && (it as Preview).documentId == docId }) {
                                        backStack.add(Preview(docId))
                                    }
                                }
                            )
                        }

                        is Preview -> NavEntry(key = key) {
                            val previewVm: PreviewViewModel = koinViewModel(
                                key        = "preview_${key.documentId}",
                                parameters = { parametersOf(key.documentId) }
                            )
                            PreviewScreen(
                                documentId = key.documentId,
                                viewModel  = previewVm,
                                onBack     = { backStack.removeLastOrNull() },
                                onAddMore  = { backStack.add(Capture(documentId = key.documentId)) },
                                onSign     = { backStack.add(Sign) },
                                onEditPage = { pageId, imagePath ->
                                    backStack.add(ImageEdit(pageId = pageId, imagePath = imagePath, documentId = key.documentId))
                                }
                            )
                        }

                        is ImageEdit -> NavEntry(key = key) {
                            val previewVm: PreviewViewModel = koinViewModel(
                                key        = "preview_${key.documentId}",
                                parameters = { parametersOf(key.documentId) }
                            )
                            ImageEditScreen(
                                pageId    = key.pageId,
                                imagePath = key.imagePath,
                                allPagesFlow = previewVm.pages,
                                onBack    = {
                                    previewVm.notifyPageEdited(key.pageId)
                                    backStack.removeLastOrNull()
                                },
                                onDrawNew = { backStack.add(DrawSign) },
                                onPageNavigate = { pageId, imagePath ->
                                    backStack.removeLastOrNull()
                                    backStack.add(ImageEdit(pageId = pageId, imagePath = imagePath, documentId = key.documentId))
                                },
                                onToolSelected = { tool ->
                                    backStack.add(PageToolEdit(
                                        pageId      = key.pageId,
                                        imagePath   = key.imagePath,
                                        documentId  = key.documentId,
                                        initialTool = tool
                                    ))
                                }
                            )
                        }

                        is PageToolEdit -> NavEntry(key = key) {
                            val previewVm: PreviewViewModel = koinViewModel(
                                key        = "preview_${key.documentId}",
                                parameters = { parametersOf(key.documentId) }
                            )
                            com.nirv.converttopdf.ui.imageedit.PageToolEditScreen(
                                pageId      = key.pageId,
                                imagePath   = key.imagePath,
                                initialTool = key.initialTool,
                                onBack      = {
                                    previewVm.notifyPageEdited(key.pageId)
                                    backStack.removeLastOrNull()
                                },
                                onDrawNew   = { backStack.add(DrawSign) }
                            )
                        }

                        is Sign -> NavEntry(key = Sign) {
                            SignatureScreen(
                                onBack    = { backStack.removeLastOrNull() },
                                onDrawNew = { backStack.add(DrawSign) }
                            )
                        }

                        is DrawSign -> NavEntry(key = DrawSign) {
                            DrawSignatureScreen(
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }

                        is Settings -> NavEntry(key = Settings) {
                            SettingsScreen(
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }

                        is Export -> NavEntry(key = Export) {
                            ExportScreen(
                                onBack    = { backStack.removeLastOrNull() },
                                onNewScan = {
                                    backStack.clear()
                                    backStack.add(Home)
                                }
                            )
                        }

                        is DirectoryFiles -> NavEntry(key = DirectoryFiles) {
                            DirectryFileScreen(
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }

                        else -> NavEntry(key = Unit) {}
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlazoBottomNav — lime accent selected, minimal divider
// ─────────────────────────────────────────────────────────────────────────────

private val navItemColors
    @Composable get() = NavigationBarItemDefaults.colors(
        selectedIconColor   = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor   = MaterialTheme.colorScheme.primary,
        unselectedIconColor = PlazoMuted,
        unselectedTextColor = PlazoMuted,
        indicatorColor      = MaterialTheme.colorScheme.primary
    )

@Composable
private fun PlazoBottomNav(
    currentDest: Any?,
    onHome: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit
) {
    val isHome     = currentDest is Home
    val isFiles    = currentDest is DirectoryFiles
    val isSettings = currentDest is Settings

    Column {
        HorizontalDivider(
            color     = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = isHome,
                onClick  = onHome,
                icon = {
                    Icon(
                        imageVector        = Icons.Default.Home,
                        contentDescription = "Inicio",
                        modifier           = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text       = "Inicio",
                        fontWeight = if (isHome) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                alwaysShowLabel = true,
                colors = navItemColors
            )

            NavigationBarItem(
                selected = isFiles,
                onClick  = onFiles,
                icon = {
                    Icon(
                        imageVector        = Icons.Default.Folder,
                        contentDescription = "Archivos",
                        modifier           = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text       = "Archivos",
                        fontWeight = if (isFiles) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                alwaysShowLabel = true,
                colors = navItemColors
            )

            NavigationBarItem(
                selected = isSettings,
                onClick  = onSettings,
                icon = {
                    Icon(
                        imageVector        = Icons.Default.Settings,
                        contentDescription = "Ajustes",
                        modifier           = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text       = "Ajustes",
                        fontWeight = if (isSettings) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                alwaysShowLabel = true,
                colors = navItemColors
            )
        }
    }
}
