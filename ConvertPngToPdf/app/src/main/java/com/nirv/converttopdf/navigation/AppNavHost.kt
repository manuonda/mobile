package com.nirv.converttopdf.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.nirv.converttopdf.ui.capture.CaptureScreen
import com.nirv.converttopdf.ui.export.ExportScreen
import com.nirv.converttopdf.ui.home.HomeScreen
import com.nirv.converttopdf.ui.preview.PreviewScreen

@Composable
fun AppNavHost() {
    val backStack = remember { mutableStateListOf<Any>(Home) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when(key) {
                is Home -> NavEntry(key = Home) {
                    HomeScreen(
                        onScanNew = { backStack.add(Capture) },
                        onFromGallery = { backStack.add(Capture) },
                        onPreview = { backStack.add(Preview) }
                    )
                }
                
                is Capture -> NavEntry(key = Capture) {
                    CaptureScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onImagesCaptured = { backStack.add(Preview) }
                    )
                }
                
                is Preview -> NavEntry(key = Preview) {
                    PreviewScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onExport = { backStack.add(Export) }
                    )
                }
                
                is Export -> NavEntry(key = Export) {
                    ExportScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onNewScan = {
                            backStack.clear()
                            backStack.add(Home)
                        }
                    )
                }
                
                else -> NavEntry(key = Unit) {}
            }
        }
    )
}
