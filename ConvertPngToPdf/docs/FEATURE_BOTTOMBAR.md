# Feature: Bottom Action Bar + Firma de Documentos

## Descripción
Agregar una barra de acciones inferior al `PreviewScreen` que replique el flujo UX de CamScanner, permitiendo al usuario **añadir más imágenes**, **compartir como PDF** y **firmar el documento** sin salir de la pantalla de previsualización.

## Referencia visual
| Pantalla | Descripción |
|----------|-------------|
| `docs/screens/1.jpeg` | Scanner ML Kit activo con badge de conteo |
| `docs/screens/2.jpeg` | Scanner con múltiples páginas capturadas |
| `docs/screens/3.jpeg` | Vista post-captura con lista de imágenes |
| `docs/screens/4.jpeg` | Bottom action bar: Añadir / Editar / Compartir / A Word / Firmar |

---

## Diseño UX/UI

### PreviewScreen (rediseño)
```
┌────────────────────────────────┐
│  ← Imágenes (2)   [🗑] [⋮]   │  TopAppBar
├────────────────────────────────┤
│                                │
│  ┌─────────────────────────┐  │
│  │ ① [thumbnail]        ✕ │  │  ElevatedCard, RoundedCorner 12dp
│  └─────────────────────────┘  │  Badge numérico overlay
│                                │
│  ┌─────────────────────────┐  │
│  │ ② [thumbnail]        ✕ │  │
│  └─────────────────────────┘  │
│                                │
├────────────────────────────────┤
│  📷        📤         ✍       │  NavigationBar
│ Añadir  Compartir   Firmar    │  (icono + label)
└────────────────────────────────┘
```

### SignatureScreen (nueva)
```
┌────────────────────────────────┐
│  ← Firmar Documento            │  TopAppBar
├────────────────────────────────┤
│                                │
│  ┌─────────────────────────┐  │
│  │                         │  │
│  │   [área de firma libre] │  │  Canvas con PointerInput
│  │                         │  │  Línea negra, stroke 4dp
│  └─────────────────────────┘  │
│                                │
├────────────────────────────────┤
│   [Limpiar]      [Aplicar]    │  BottomAppBar
└────────────────────────────────┘
```

---

## Archivos a crear

| Archivo | Descripción |
|---------|-------------|
| `ui/signature/SignatureScreen.kt` | Pantalla con canvas táctil para dibujar firma |
| `ui/signature/SignatureViewModel.kt` | Estado de paths, lógica de superposición sobre bitmap |

---

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `ui/preview/PreviewScreen.kt` | Eliminar FAB, agregar `NavigationBar`, mejorar cards, nuevos callbacks `onAddMore` y `onSign` |
| `ui/preview/PreviewViewModel.kt` | Inyectar `ExportToPdfUseCase`, agregar `shareAsPdf()` con `ShareState` |
| `navigation/AppDestination.kt` | Agregar `data object Sign` |
| `navigation/AppNavHost.kt` | Navegación inteligente Capture→Preview (sin duplicar), agregar ruta `Sign` |
| `di/AppModule.kt` | Actualizar `PreviewViewModel(get(), get())`, registrar `SignatureViewModel` |

---

## Flujo de navegación

```
[Home]
  └─▶ [Capture]  ←──────────────────────────┐
        └─▶ [Preview]  (workspace principal) │
              ├─ [📷 Añadir]    ─────────────┘  vuelve al mismo Preview al terminar
              ├─ [📤 Compartir] ──▶ genera PDF (IO) → Intent.ACTION_SEND
              └─ [✍ Firmar]     ──▶ [SignatureScreen]
                                       └─ [Aplicar] ──▶ firma superpuesta en última imagen
                                                         → vuelve a Preview
```

---

## Detalle técnico por componente

### PreviewViewModel — nuevo estado de compartir
```kotlin
sealed class ShareState {
    data object Idle    : ShareState()
    data object Loading : ShareState()
    data class  Ready(val uri: Uri)   : ShareState()
    data class  Error(val msg: String): ShareState()
}

// shareAsPdf() corre en IO, emite Ready(uri) para que la Screen lance el Intent
```

### SignatureViewModel — aplicar firma
```kotlin
// applySignatureToLastImage():
//   1. Toma el último Bitmap del ImageRepository
//   2. Crea un Canvas sobre ese Bitmap mutable
//   3. Dibuja todos los Path guardados
//   4. Reemplaza el bitmap en el repositorio via removeImage + addImage
```

### AppNavHost — Capture inteligente
```kotlin
// Si Preview ya está en el back stack:
//   onImagesCaptured → removeLastOrNull()  (quita Capture, vuelve al Preview existente)
// Si NO está:
//   onImagesCaptured → backStack.add(Preview)
```

---

## Criterios de aceptación

- [ ] Bottom bar visible con 3 acciones (Añadir, Compartir, Firmar) con ícono + label
- [ ] "Añadir" navega a Capture y al terminar vuelve al **mismo** Preview sin duplicar pantallas
- [ ] "Compartir" genera PDF en background (spinner visible), luego abre el selector del sistema
- [ ] "Firmar" abre canvas táctil; el trazo es visible mientras se dibuja
- [ ] "Limpiar" borra todos los trazos del canvas
- [ ] "Aplicar" superpone la firma en la última imagen y regresa al Preview
- [ ] La firma queda visible en el thumbnail de la última imagen en el listado
- [ ] Compilación sin errores: `./gradlew assembleDebug`

---

## Estimación de tareas

| Tarea | Prioridad |
|-------|-----------|
| Rediseño `PreviewScreen` + `NavigationBar` | Alta |
| Lógica `shareAsPdf` en `PreviewViewModel` | Alta |
| Navegación inteligente Capture↔Preview | Alta |
| `SignatureScreen` + canvas táctil | Media |
| `SignatureViewModel` + superposición de firma | Media |
| Registro en `AppModule` + `AppDestination` | Baja |
