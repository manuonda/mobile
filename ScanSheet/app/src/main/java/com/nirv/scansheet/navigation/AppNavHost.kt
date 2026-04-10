package com.nirv.scansheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.nirv.scansheet.ui.capture.CaptureScreen
import com.nirv.scansheet.ui.export.ExportScreen
import com.nirv.scansheet.ui.home.HomeScreen
import com.nirv.scansheet.ui.preview.PreviewScreen

/**
 * AppNavHost es el orquestador principal de la navegación en la aplicación.
 * Utiliza Navigation3 para gestionar el cambio entre pantallas.
 */
@Composable
fun AppNavHost() {
    // Definimos el backStack (pila de navegación). 
    // Empezamos con la pantalla 'Home' como destino inicial.
    val backStack = remember { mutableStateListOf<Any>(Home) }

    // NavDisplay es el componente que renderiza la pantalla actual basándose en el backStack.
    NavDisplay(
        backStack = backStack,
        // Acción a realizar cuando el usuario presiona el botón 'Atrás' del sistema.
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            // El 'key' representa el destino actual en la pila.
            when(key) {
                // Definición para la pantalla Principal
                is Home -> NavEntry(key = Home) {
                    HomeScreen (
                        onScanNew = { backStack.add(Capture) }, // Navega a Captura
                        onFromGallery = { /* TODO: Implementar selección de galería */ },
                        onSettings = { /* TODO: Implementar ajustes */ }
                    )
                }
                
                // Definición para la pantalla de Captura (Cámara)
                is Capture -> NavEntry(key = Capture) {
                    CaptureScreen(
                        onBack = { backStack.removeLastOrNull() }, // Vuelve a Home
                        onTextRecognized = { backStack.add(Preview) } // Navega a Previsualización
                    )
                }
                
                // Definición para la pantalla de Previsualización de resultados
                is Preview -> NavEntry(key = Preview) {
                    PreviewScreen(
                        onBack = {backStack.removeLastOrNull()},
                        onExport = {backStack.add(Export)}
                    )
                }
                
                // Definición para la pantalla de Exportación (Excel)
                is Export -> NavEntry(key = Export) {
                    ExportScreen(
                         onBack = { backStack.removeLastOrNull() },
                         onNewScan = {
                             // Resetea a [Home, Capture] para que ← en Capture vuelva a Home
                             backStack.clear()
                             backStack.add(Home)
                             backStack.add(Capture)
                         }
                     )
                }
                
                // Caso por defecto para evitar errores de compilación
                else -> NavEntry(key = Unit) {}
            }
        }
    )
}

