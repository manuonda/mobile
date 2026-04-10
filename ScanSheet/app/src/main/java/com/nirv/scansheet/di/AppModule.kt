package com.nirv.scansheet.di

import com.nirv.scansheet.data.ScanRepository
import com.nirv.scansheet.data.ocr.MlKitOcrProcessor
import com.nirv.scansheet.data.ocr.TextStructureParser
import com.nirv.scansheet.domain.usecase.ProcessOcrResultUseCase
import com.nirv.scansheet.data.excel.ExcelExporter
import com.nirv.scansheet.domain.usecase.ExportToExcelUseCase
import com.nirv.scansheet.ui.capture.CaptureViewModel
import com.nirv.scansheet.ui.export.ExportViewModel
import com.nirv.scansheet.ui.preview.PreviewViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Módulo Koin que declara cómo construir cada dependencia.
 * single   → una sola instancia compartida (equivale a @Singleton)
 * viewModel → Koin crea y gestiona el ciclo de vida del ViewModel
 *
 * get() → Koin resuelve automáticamente la dependencia ya declarada en el módulo.
 */
val appModule = module {

    single { ScanRepository() }
    single { TextStructureParser() }
    single { MlKitOcrProcessor() }
    single { ProcessOcrResultUseCase(get(), get()) }
    single { ExcelExporter(androidContext()) }
    single { ExportToExcelUseCase(get(), get()) }

    viewModel { CaptureViewModel(get(), get()) }
    viewModel { PreviewViewModel(get()) }
    viewModel { ExportViewModel(get()) }
}
