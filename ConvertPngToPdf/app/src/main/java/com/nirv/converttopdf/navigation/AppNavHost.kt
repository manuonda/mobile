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
import com.nirv.converttopdf.ui.settings.SettingsScreen
import com.nirv.converttopdf.ui.signature.DrawSignatureScreen
import com.nirv.converttopdf.ui.signature.SignatureScreen

@Composable
fun AppNavHost() {
    val backStack = remember { mutableStateListOf<Any>(Home) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is Home -> NavEntry(key = Home) {
                    HomeScreen(
                        onScanNew  = { backStack.add(Capture) },
                        onFiles    = { backStack.add(Capture) },
                        onSettings = { backStack.add(Settings) }
                    )
                }

                is Capture -> NavEntry(key = Capture) {
                    CaptureScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onImagesCaptured = {
                            if (backStack.contains(Preview)) {
                                backStack.removeLastOrNull()
                            } else {
                                backStack.add(Preview)
                            }
                        }
                    )
                }

                is Preview -> NavEntry(key = Preview) {
                    PreviewScreen(
                        onBack    = { backStack.removeLastOrNull() },
                        onAddMore = { backStack.add(Capture) },
                        onSign    = { backStack.add(Sign) }
                    )
                }

                // SignatureScreen: documento + overlay arrastrable + sheets
                is Sign -> NavEntry(key = Sign) {
                    SignatureScreen(
                        onBack    = { backStack.removeLastOrNull() },
                        onDrawNew = { backStack.add(DrawSign) }
                    )
                }

                // DrawSignatureScreen: canvas para crear una firma nueva
                // Al guardar, SignatureViewModel lo ve reactivamente vía SignatureRepository
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

                else -> NavEntry(key = Unit) {}
            }
        }
    )
}
