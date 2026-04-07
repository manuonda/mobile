# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What is ScanSheet

Android app that converts physical documents (price lists, inventories, handwritten notes) into `.xlsx` files using the device camera and **offline OCR** (ML Kit). No internet or external servers required.

Main flow: `Camera → OCR (ML Kit) → Review/Edit → .xlsx (Apache POI) → Share`

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.nirv.scansheet.data.TextStructureParserTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Project state

The project is currently in **Phase 1 (Setup)**. `MainActivity` contains only a placeholder "Hello Android" screen. The architecture described below is the target design — most of it is not yet implemented.

## Package & SDK

- Package: `com.nirv.scansheet`
- `minSdk = 27`, `targetSdk = 36`
- Kotlin 2.2.10, AGP 9.1.0

## Target Architecture (MVVM)

Three screens, each with its own `Screen.kt` + `ViewModel.kt`:

| Layer | Package | Role |
|---|---|---|
| UI | `ui/capture/`, `ui/preview/`, `ui/export/` | Jetpack Compose screens, observe `StateFlow` from ViewModel |
| Domain | `domain/usecase/` | `ProcessOcrResultUseCase`, `ExportToExcelUseCase` |
| Data | `data/ocr/`, `data/excel/` | `MlKitOcrProcessor`, `TextStructureParser`, `ExcelExporter` |
| DI | `di/AppModule.kt` | Hilt `@Module` |
| Shared state | `data/ScanRepository.kt` | `@Singleton` in-memory store — the only way to pass `List<ExcelRow>` between ViewModels across screens |

### Why `ScanRepository` for inter-screen data

Navigation 3 routes are `data object`s (no typed arguments). Passing a large `List<String>` via route params is not viable. The canonical solution: `CaptureViewModel` writes to `ScanRepository`, `PreviewViewModel` and `ExportViewModel` read from it. Hilt injects the same singleton instance to all.

### UiState pattern

Every ViewModel exposes a `sealed class XxxUiState` as a `StateFlow`. Screens collect it with `collectAsStateWithLifecycle()`. Always use `Loading / Success / Error` states; the `CaptureViewModel` also has `Idle` and `Done` (triggers navigation).

### Navigation 3

Back stack is a `SnapshotStateList` you control directly — not an opaque `NavController`.

```kotlin
val backStack = rememberNavBackStack(Capture)
backStack.add(Preview)          // navigate forward
backStack.removeLastOrNull()    // go back
backStack.clear(); backStack.add(Capture)  // reset to root
```

Destinations are `@Serializable data object`s implementing `NavKey` in `navigation/AppDestinations.kt`.

## Dependencies to add (not yet in `libs.versions.toml`)

Per `docs/README.md`, these must be added before implementing each phase:

```toml
composeBom              = "2026.03.00"
navigation3             = "1.1.0-rc01"
camerax                 = "1.5.1"
mlkitTextRecognition    = "16.0.1"
apachePoi               = "5.3.0"
hilt                    = "2.54"
accompanistPermissions  = "0.37.0"
coroutines              = "1.10.1"
desugarJdkLibs          = "2.1.4"
```

## Apache POI requirements

POI requires two things in `app/build.gradle.kts` or it crashes at runtime on API < 34:

1. `isCoreLibraryDesugaringEnabled = true` in `compileOptions`
2. `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")`
3. Packaging excludes to avoid Gradle merge errors:

```kotlin
packaging {
    resources {
        excludes += "META-INF/NOTICE.md"
        excludes += "META-INF/LICENSE.md"
        excludes += "META-INF/DEPENDENCIES"
    }
}
```

## ML Kit: use bundled variant

Use `com.google.mlkit:text-recognition` (bundled), **not** the GMS unbundled variant. The bundled model ships in the APK and works on first launch without internet.

## Critical unit test

`TextStructureParser` (converts raw OCR lines → `List<ExcelRow>`) is the most logic-heavy class and the one most likely to break on edge cases. Always cover it with unit tests in `src/test/…/data/TextStructureParserTest.kt`.
