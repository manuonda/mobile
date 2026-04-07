# ScanSheet

> Digitalización de documentos físicos → Excel `.xlsx` · 100% offline · Android nativo

**ScanSheet** es una herramienta técnica de digitalización. Convierte listas de precios,
inventarios y notas escritas a mano en archivos `.xlsx` estructurados usando la cámara del
dispositivo y OCR local, sin necesidad de internet ni servidores externos.

---

## Índice

1. [Concepto y alcance del MVP](#1-concepto-y-alcance-del-mvp)
2. [Stack tecnológico](#2-stack-tecnológico)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Arquitectura MVVM](#4-arquitectura-mvvm)
5. [Navegación — Navigation 3](#5-navegación--navigation-3)
6. [Pantallas y funcionalidades](#6-pantallas-y-funcionalidades)
7. [Flujo de datos](#7-flujo-de-datos)
8. [Plan de trabajo — 5 fases](#8-plan-de-trabajo--5-fases)
9. [Configuración del proyecto](#9-configuración-del-proyecto)
10. [Decisiones técnicas clave](#10-decisiones-técnicas-clave)
11. [Recursos de referencia](#11-recursos-de-referencia)

---

## 1. Concepto y alcance del MVP

### Flujo principal

```
[ Documento físico ] → [ Cámara ] → [ OCR offline ] → [ Revisión ] → [ .xlsx ]
```

### Tipos de contenido soportados

| Tipo | Ejemplo | Estructura esperada en Excel |
|---|---|---|
| Lista de precios | Tornillo 5mm · $850 | Columna A: producto · Columna B: precio |
| Inventario / stock | Camiseta M · 24 unidades | Columna A: ítem · Columna B: cantidad |
| Texto libre / notas | Anotaciones varias | Una celda por línea reconocida |

### Fuera del MVP (no incluir)

- Historial de escaneos
- Formato especial en Excel (negritas, colores, fórmulas)
- Múltiples hojas por escaneo
- Reconocimiento de tablas con bordes
- Sincronización en la nube
- Autenticación de usuarios

---

## 2. Stack tecnológico

| Capa | Tecnología | Versión | Justificación |
|---|---|---|---|
| Lenguaje | Kotlin | 2.1.0 | Nativo Android, sintaxis cercana a Java |
| UI | Jetpack Compose + Material 3 | BOM 2026.03.00 | Declarativo (similar a Angular), sin XML |
| Navegación | Navigation 3 | 1.1.0-rc01 | Compose-first, control total del back stack |
| Cámara | CameraX | 1.5.1 | Enfoque automático optimizado para texto |
| OCR | ML Kit Text Recognition v2 | 16.0.1 | 100% offline, bundled, sin API key |
| Excel | Apache POI | 5.3.0 | Estándar industria, API familiar desde Java |
| Inyección de dependencias | Hilt | 2.54 | DI simple en ViewModels y repositorios |
| Coroutines | Kotlinx Coroutines | 1.10.1 | Async sin callbacks — similar a async/await |
| Permisos | Accompanist Permissions | 0.37.0 | Manejo declarativo de permisos en Compose |
| Build system | Gradle KTS + Version Catalog | AGP 8.7.3 | Tipado, autocompletado, una fuente de verdad |

### ¿Por qué Navigation 3 y no Navigation 2?

Navigation 3 (estable desde noviembre 2025) fue diseñado específicamente para Compose.
La diferencia clave: el back stack es una `SnapshotStateList` que vos controlás directamente,
en lugar de un `NavController` opaco. Para 3 pantallas sin deep links complejos, es ideal.

```kotlin
// Navigation 3 — el back stack es tuyo
val backStack = rememberNavBackStack(Capture)   // estado observable normal de Compose

// Navegar = agregar al stack
backStack.add(Preview)

// Volver = sacar del stack
backStack.removeLastOrNull()
```

---

## 3. Estructura del proyecto

```
scansheet/
├── gradle/
│   └── libs.versions.toml              # Version Catalog — una sola fuente de versiones
├── build.gradle.kts                    # Plugins raíz (apply false)
│
└── app/
    ├── build.gradle.kts                # Dependencias del módulo app
    ├── src/main/
    │   ├── AndroidManifest.xml
    │   ├── res/
    │   │   └── xml/
    │   │       └── file_provider_paths.xml
    │   │
    │   └── java/com/tuapp/scansheet/
    │       │
    │       ├── ScanSheetApp.kt         # @HiltAndroidApp — punto de entrada de Hilt
    │       ├── MainActivity.kt         # @AndroidEntryPoint — host de Compose
    │       │
    │       ├── navigation/
    │       │   ├── AppDestinations.kt  # Sealed class con las 3 rutas (NavKey)
    │       │   └── AppNavHost.kt       # NavDisplay + entryProvider
    │       │
    │       ├── ui/
    │       │   ├── theme/
    │       │   │   ├── Theme.kt        # ScanSheetTheme — Dynamic Color + dark mode
    │       │   │   ├── Color.kt        # Generado por Material Theme Builder
    │       │   │   └── Type.kt         # Tipografía M3
    │       │   │
    │       │   ├── capture/            # Pantalla 1
    │       │   │   ├── CaptureScreen.kt
    │       │   │   └── CaptureViewModel.kt
    │       │   │
    │       │   ├── preview/            # Pantalla 2
    │       │   │   ├── PreviewScreen.kt
    │       │   │   └── PreviewViewModel.kt
    │       │   │
    │       │   └── export/             # Pantalla 3
    │       │       ├── ExportScreen.kt
    │       │       └── ExportViewModel.kt
    │       │
    │       ├── domain/
    │       │   ├── model/
    │       │   │   ├── ScannedDocument.kt   # data class — resultado del OCR
    │       │   │   └── ExcelRow.kt          # data class — fila lista para exportar
    │       │   └── usecase/
    │       │       ├── ProcessOcrResultUseCase.kt
    │       │       └── ExportToExcelUseCase.kt
    │       │
    │       ├── data/
    │       │   ├── ocr/
    │       │   │   ├── MlKitOcrProcessor.kt      # ML Kit → List<String>
    │       │   │   └── TextStructureParser.kt     # List<String> → List<ExcelRow>
    │       │   └── excel/
    │       │       └── ExcelExporter.kt           # List<ExcelRow> → .xlsx (Apache POI)
    │       │
    │       └── di/
    │           └── AppModule.kt        # Hilt @Module — provee OCR processor, exporter
    │
    └── src/test/
        └── java/com/tuapp/scansheet/
            └── data/
                └── TextStructureParserTest.kt   # Tests del parser — crítico
```

---

## 4. Arquitectura MVVM

Cada pantalla sigue el mismo patrón. Los ViewModels no conocen la UI, la UI no conoce
la lógica de negocio.

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer (Compose)                                     │
│                                                         │
│  CaptureScreen ──observa──► CaptureViewModel            │
│       │                          │                      │
│       │ eventos (click)          │ UiState               │
│       └──────────────────────────┘                      │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼ llama
┌─────────────────────────────────────────────────────────┐
│  Domain Layer                                           │
│                                                         │
│  ProcessOcrResultUseCase                                │
│  ExportToExcelUseCase                                   │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼ usa
┌─────────────────────────────────────────────────────────┐
│  Data Layer                                             │
│                                                         │
│  MlKitOcrProcessor   TextStructureParser   ExcelExporter│
└─────────────────────────────────────────────────────────┘
```

### UiState — patrón sealed class

Cada ViewModel expone un `UiState` como `StateFlow`. La pantalla lo observa con
`collectAsStateWithLifecycle()`.

```kotlin
// Ejemplo para PreviewViewModel
sealed class PreviewUiState {
    data object Loading : PreviewUiState()
    data class Success(val rows: List<ExcelRow>) : PreviewUiState()
    data class Error(val message: String) : PreviewUiState()
}

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val processOcrResult: ProcessOcrResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    fun processText(rawLines: List<String>) {
        viewModelScope.launch {
            _uiState.value = PreviewUiState.Loading
            runCatching { processOcrResult(rawLines) }
                .onSuccess { _uiState.value = PreviewUiState.Success(it) }
                .onFailure { _uiState.value = PreviewUiState.Error(it.message ?: "Error") }
        }
    }
}
```

---

## 5. Navegación — Navigation 3

### Destinos

```kotlin
// navigation/AppDestinations.kt

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Capture : NavKey       // Pantalla 1 — sin parámetros

@Serializable
data object Preview : NavKey       // Pantalla 2 — datos compartidos via ViewModel

@Serializable
data object Export : NavKey        // Pantalla 3 — datos compartidos via ViewModel
```

### NavDisplay

```kotlin
// navigation/AppNavHost.kt

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(Capture)

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSceneNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Capture> {
                CaptureScreen(
                    onTextRecognized = { backStack.add(Preview) }
                )
            }
            entry<Preview> {
                PreviewScreen(
                    onBack   = { backStack.removeLastOrNull() },
                    onExport = { backStack.add(Export) }
                )
            }
            entry<Export> {
                ExportScreen(
                    onBack    = { backStack.removeLastOrNull() },
                    onNewScan = {
                        // Limpia el stack y vuelve a Capture
                        backStack.clear()
                        backStack.add(Capture)
                    }
                )
            }
        }
    )
}
```

### ¿Por qué los datos no pasan como parámetros de ruta?

El OCR devuelve una lista de strings que puede ser larga. Pasar listas grandes
en rutas de navegación es frágil (límites de Intent, serialización). La solución
correcta: el `PreviewViewModel` recibe los datos a través de un repositorio en memoria
compartido via Hilt, que `CaptureViewModel` escribe y `PreviewViewModel` lee.

---

## 6. Pantallas y funcionalidades

### Pantalla 1 — Capture

**Ruta:** `Capture` (data object)
**ViewModel:** `CaptureViewModel`

```
┌─────────────────────────────┐
│  ← [TopAppBar: ScanSheet]   │
├─────────────────────────────┤
│                             │
│   ┌─────────────────────┐   │
│   │                     │   │
│   │   CameraX Preview   │   │
│   │                     │   │
│   │  ┌─┐           ┌─┐  │   │
│   │  └─┘           └─┘  │   │
│   │                     │   │
│   │  ┌─┐           ┌─┐  │   │
│   │  └─┘           └─┘  │   │
│   │                     │   │
│   └─────────────────────┘   │
│                             │
│   Flash •  Galería          │
│                             │
│   [ Capturar ]              │
└─────────────────────────────┘
```

**Funcionalidades:**
- Visor CameraX a pantalla completa con `CameraXViewfinder`
- Guías de encuadre dibujadas con `Canvas` (4 esquinas)
- Botón "Capturar" → dispara `ImageCapture`, pasa el Bitmap al OCR
- Acceso a galería con `ActivityResultContracts.PickVisualMedia`
- Toggle de flash (`CameraControl.enableTorch()`)
- Solicitud de permiso de cámara con Accompanist al iniciar
- Indicador de carga mientras corre el OCR (circular progress)

**UiState:**
```kotlin
sealed class CaptureUiState {
    data object Idle : CaptureUiState()
    data object Processing : CaptureUiState()   // OCR corriendo
    data class Error(val msg: String) : CaptureUiState()
    data object Done : CaptureUiState()         // trigger navegación a Preview
}
```

---

### Pantalla 2 — Preview / Edición

**Ruta:** `Preview` (data object)
**ViewModel:** `PreviewViewModel`

```
┌─────────────────────────────┐
│  ← [Revisar datos]   3 filas│
├─────────────────────────────┤
│ Producto A · $1200      [✎] │
├─────────────────────────────┤
│ Producto B · $850       [✎] │
├─────────────────────────────┤
│ Producto C · $3400      [✎] │
├─────────────────────────────┤
│ [+ Agregar fila]            │
│                             │
│                             │
│ [ Generar Excel → ]         │
└─────────────────────────────┘
```

**Funcionalidades:**
- `LazyColumn` con una card por fila reconocida
- Edición inline con `TextField` — tap en la celda la vuelve editable
- Eliminar fila con swipe-to-dismiss (`SwipeToDismissBox` de M3)
- Botón "+ Agregar fila" → inserta una fila vacía al final
- Contador de filas en el TopAppBar
- Botón "Generar Excel" → dispara exportación y navega a Export

**UiState:**
```kotlin
sealed class PreviewUiState {
    data object Loading : PreviewUiState()
    data class Success(
        val rows: List<ExcelRow>,
        val isEditing: Boolean = false
    ) : PreviewUiState()
    data class Error(val msg: String) : PreviewUiState()
}
```

---

### Pantalla 3 — Export

**Ruta:** `Export` (data object)
**ViewModel:** `ExportViewModel`

```
┌─────────────────────────────┐
│  ← [Exportar]               │
├─────────────────────────────┤
│  Nombre del archivo         │
│  [lista_precios_06abr     ] │
│                             │
│  📊 lista_precios_06abr.xlsx│
│     3 filas · listo         │
│                             │
│  Compartir vía              │
│  [WhatsApp] [Gmail]         │
│  [Drive]    [Copiar]        │
│                             │
│ [ Guardar en dispositivo ]  │
│ [ Nuevo escaneo ]           │
└─────────────────────────────┘
```

**Funcionalidades:**
- Campo de nombre de archivo editable (pre-relleno con fecha actual)
- Botón "Guardar en dispositivo" → escribe en `getExternalFilesDir()`
- Botón "Compartir" → `Intent.ACTION_SEND` con `FileProvider` URI
  - Abre el Share Sheet nativo de Android (WhatsApp, Gmail, Drive, etc.)
- Snackbar de éxito / error
- Botón "Nuevo escaneo" → limpia el stack y vuelve a Capture

**UiState:**
```kotlin
sealed class ExportUiState {
    data object Idle : ExportUiState()
    data object Exporting : ExportUiState()
    data class Success(val filePath: String) : ExportUiState()
    data class Error(val msg: String) : ExportUiState()
}
```

---

## 7. Flujo de datos

```
CaptureScreen
    │
    │ Bitmap (foto capturada)
    ▼
MlKitOcrProcessor.kt
    │  TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    │  .process(InputImage.fromBitmap(bitmap, 0)).await()
    │
    │ List<String>  ← líneas de texto reconocidas
    ▼
TextStructureParser.kt
    │  Detecta separadores (espacios múltiples, guiones, ·, $)
    │  Convierte cada línea en una lista de columnas
    │
    │ List<ExcelRow>  ← filas estructuradas
    ▼
ScanRepository.kt (in-memory, @Singleton via Hilt)
    │  Almacena las filas mientras dura la sesión
    │
    ├──────────────────────► PreviewViewModel (lee las filas)
    │                              │
    │                              │ Usuario edita, agrega, elimina filas
    │                              ▼
    │                         List<ExcelRow> actualizada
    │
    └──────────────────────► ExportViewModel (lee filas editadas)
                                   │
                                   ▼
                             ExcelExporter.kt (Apache POI)
                                   │
                                   │ .xlsx en getExternalFilesDir()
                                   ▼
                             FileProvider URI
                                   │
                             Intent.ACTION_SEND → Share Sheet
```

---

## 8. Plan de trabajo — 5 fases

| Fase | Nombre | Tareas principales | Estimación |
|---|---|---|---|
| 1 | Setup del proyecto | Crear proyecto, configurar BOM, tema M3, estructura de carpetas | 2–3 días |
| 2 | Pantallas — estructura | Las 3 pantallas con UI placeholder + Navigation 3 funcionando | 3–4 días |
| 3 | Motor OCR | `MlKitOcrProcessor` + `TextStructureParser` + permisos | 4–5 días |
| 4 | Generador Excel | `ExcelExporter` (Apache POI) + FileProvider + Share Intent | 2–3 días |
| 5 | Pulido y pruebas | UiState de error/carga, unit tests del parser, prueba en dispositivo | 2–3 días |

**Total estimado: ~3–4 semanas**

### Fase 1 — Setup (detalle)

- [ ] Crear proyecto en Android Studio (Empty Activity, Kotlin, minSdk 26)
- [ ] Copiar `libs.versions.toml` y `build.gradle.kts` (ya generados)
- [ ] Sync Gradle — verificar que todas las dependencias resuelven
- [ ] Configurar tema en [Material Theme Builder](https://m3.material.io/theme-builder)
      → exportar → reemplazar `Color.kt`
- [ ] Verificar que `ScanSheetTheme` aplica el tema en el emulador
- [ ] Crear carpetas vacías de la estructura definida arriba

### Fase 2 — Pantallas (detalle)

- [ ] Crear `AppDestinations.kt` con los 3 `NavKey`
- [ ] Crear `AppNavHost.kt` con `NavDisplay` y 3 entradas
- [ ] Implementar `CaptureScreen` placeholder (botón que navega a Preview)
- [ ] Implementar `PreviewScreen` placeholder (lista hardcodeada + botón a Export)
- [ ] Implementar `ExportScreen` placeholder (nombre de archivo + botón volver)
- [ ] Verificar navegación completa en emulador

### Fase 3 — Motor OCR (detalle)

- [ ] Implementar `MlKitOcrProcessor` con `ImageAnalysis` de CameraX
- [ ] Integrar `CameraXViewfinder` en `CaptureScreen` (reemplazar placeholder)
- [ ] Implementar `TextStructureParser` con heurísticas de separadores
- [ ] Crear `ScanRepository` in-memory con `@Singleton`
- [ ] Manejar permisos de cámara con Accompanist
- [ ] Conectar `CaptureViewModel` → OCR → Repository → navegar a Preview
- [ ] Test manual: escanear una lista de precios real

### Fase 4 — Excel (detalle)

- [ ] Implementar `ExcelExporter` con Apache POI (`XSSFWorkbook`)
- [ ] Configurar `FileProvider` en `AndroidManifest`
- [ ] Implementar guardado en `getExternalFilesDir("Documents/ScanSheet/")`
- [ ] Implementar `Intent.ACTION_SEND` con URI del FileProvider
- [ ] Conectar `ExportViewModel` → ExcelExporter → Share Sheet

### Fase 5 — Pulido (detalle)

- [ ] Agregar loading indicators en los 3 ViewModels
- [ ] Agregar manejo de errores con Snackbar
- [ ] Escribir `TextStructureParserTest` con al menos 5 casos reales
- [ ] Probar en dispositivo físico con distintas condiciones de iluminación
- [ ] Ajustar umbral de confianza del OCR si es necesario
- [ ] Probar compartir por WhatsApp, Gmail y guardar en Files

---

## 9. Configuración del proyecto

### Versiones confirmadas (abril 2026)

```toml
# gradle/libs.versions.toml
composeBom              = "2026.03.00"
camerax                 = "1.5.1"
mlkitTextRecognition    = "16.0.1"
apachePoi               = "5.3.0"
hilt                    = "2.54"
navigation3             = "1.1.0-rc01"
kotlin                  = "2.1.0"
agp                     = "8.7.3"
```

### Package name

```
com.tuapp.scansheet
```

Reemplazar `tuapp` con tu nombre o empresa antes de publicar en Play Store.

### minSdk

`minSdk = 26` (Android 8.0) — cubre el 95%+ de dispositivos activos en Argentina.
ML Kit Text Recognition requiere API 21+, pero Apache POI con desugaring recomienda 26+.

### Nota sobre Apache POI

POI requiere `isCoreLibraryDesugaringEnabled = true` en el `build.gradle.kts` del módulo
y la dependencia `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")`.
Sin esto, POI falla en runtime en dispositivos con API < 34 con errores de clases Java 8.

También requiere estos `excludes` en `packaging` para evitar errores de merge de Gradle:

```kotlin
packaging {
    resources {
        excludes += "META-INF/NOTICE.md"
        excludes += "META-INF/LICENSE.md"
        excludes += "META-INF/DEPENDENCIES"
    }
}
```

---

## 10. Decisiones técnicas clave

### ¿Por qué ML Kit bundled y no unbundled?

La versión **bundled** (`com.google.mlkit:text-recognition`) incluye el modelo en el APK.
Aumenta el tamaño del APK ~3–4 MB pero garantiza que el OCR funciona
**desde la primera apertura sin internet**. La versión unbundled descarga el modelo
la primera vez, lo que puede fallar si el usuario no tiene datos.

### ¿Por qué Apache POI y no una librería más liviana?

Para este MVP, Apache POI es la opción correcta porque:
- Es el estándar de la industria para `.xlsx` en Java/Kotlin
- La API es familiar si venís del mundo Java
- Soporta todas las operaciones que podríamos necesitar en versiones futuras
- El overhead de tamaño (~4 MB) es aceptable para una app de productividad

### ¿Por qué MVVM y no MVI?

Con 3 pantallas y lógica relativamente lineal, MVVM es suficiente y más simple.
MVI agrega boilerplate que no se justifica en este MVP. Si la app escala,
migrar de MVVM a MVI es incremental.

### ¿Por qué compartir el estado entre ViewModels via Repository?

Navigation 3 no tiene un mecanismo nativo para pasar datos complejos entre pantallas
en los argumentos de ruta (similar al problema de pasar objetos grandes en Intents).
La solución idiomática en Compose es un **repositorio en memoria** inyectado como
`@Singleton` por Hilt. Todos los ViewModels que necesitan esos datos los piden al
repositorio. Los datos se limpian cuando el proceso de la app termina.

---

## 11. Recursos de referencia

| Recurso | URL |
|---|---|
| Material Theme Builder | https://m3.material.io/theme-builder |
| Componentes M3 | https://m3.material.io/components |
| API Reference Compose M3 | https://composables.com/material3 |
| Navigation 3 docs | https://developer.android.com/guide/navigation/navigation3 |
| Navigation 3 recipes | https://github.com/android/nav3-recipes |
| CameraX docs | https://developer.android.com/training/camerax |
| ML Kit Text Recognition | https://developers.google.com/ml-kit/vision/text-recognition/v2/android |
| Apache POI | https://poi.apache.org/components/spreadsheet/quick-guide.html |
| Hilt DI | https://developer.android.com/training/dependency-injection/hilt-android |
| Compose BOM mapping | https://developer.android.com/jetpack/compose/bom/bom-mapping |

---

## Cómo leer este README si sos nuevo en Kotlin/Compose

Dado que tu background principal es Java, PHP y Angular, estas analogías ayudan:

| Concepto Android/Compose | Analogía desde tu stack |
|---|---|
| `@Composable fun Screen()` | Componente Angular (`@Component`) |
| `StateFlow` en ViewModel | Observable / Subject de RxJS |
| `collectAsStateWithLifecycle()` | `async pipe` en Angular |
| `Modifier` en Compose | CSS en línea encadenado |
| `LazyColumn` | `*ngFor` con virtual scrolling |
| Hilt `@Inject` | Inyección de dependencias en Angular / Spring |
| `viewModelScope.launch { }` | `async/await` en JS/TS |
| `data class ExcelRow(...)` | POJO / DTO en Java |
| `sealed class UiState` | Discriminated union de TypeScript |
| `NavKey` en Nav3 | Route en Angular Router |

---

*ScanSheet MVP — generado en abril 2026*
