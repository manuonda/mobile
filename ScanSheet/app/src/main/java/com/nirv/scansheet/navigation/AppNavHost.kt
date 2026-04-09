package com.nirv.scansheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.nirv.scansheet.ui.home.HomeScreen


@Composable
fun AppNavHost() {
    val backStack = remember { mutableStateListOf<Any>(Home) }

    NavDisplay(
        backStack = backStack,
        onBack = {backStack.removeLastOrNull()},
        entryProvider =  { key ->
            when(key) {
                is Home -> NavEntry(key = Home) {
                    HomeScreen (
                        onScanNew = {backStack.add(Capture)},
                        onFromGallery = {/* TODO  */ },
                        onSettings = { /*TODO */}
                    )
                }
                is Capture -> NavEntry(key = Capture) {
                    CaptureScreen(
                        onBack = {backStack.removeLastOrNull()},
                        onTextRecognized = {backStack.add(Preview)}

                    )
                }
                is Preview -> NavEntry(key = Preview) {
                    // PreviewScreen
                }
                is Export -> NavEntry(key = Export) {
                    // ExportScreen
                }
                else -> NavEntry(key = Unit) {}

            }
        }

    )
}