package com.nirv.scansheet.ui.capture

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable


@Composable
fun CaptureScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
        }
    ) {

    }
}


---
El CaptureScreen.kt (placeholder por ahora)

// ui/capture/CaptureScreen.kt
package com.nirv.scansheet.ui.capture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onTextRecognized: () -> Unit   // cuando el OCR termina → va a Preview
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear Documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Cámara — placeholder", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            // Botón temporal para simular OCR completo y poder navegar
            Button(onClick = onTextRecognized) {
                Text("Simular escaneo → Preview")
            }
        }
    }
}

---
Y el MainActivity.kt — conectar todo

package com.nirv.scansheet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nirv.scansheet.navigation.AppNavHost
import com.nirv.scansheet.ui.theme.ScanSheetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScanSheetTheme {
                AppNavHost()
            }
        }
    }
}

---
Resumen del flujo

MainActivity
└── AppNavHost  (backStack = [Home])
├── Home     → botón "Escanear Nuevo" → backStack.add(Capture)
├── Capture  → botón "Simular" → backStack.add(Preview)
├── Preview  → (TODO fase 2)
└── Export   → (TODO fase 2)

¿Querés que te cree los archivos o preferís hacerlo vos con este código?