package com.nirv.converttopdf.di

import com.nirv.converttopdf.data.ImageRepository
import com.nirv.converttopdf.data.PdfExporter
import com.nirv.converttopdf.domain.usecase.ExportToPdfUseCase
import com.nirv.converttopdf.ui.capture.CaptureViewModel
import com.nirv.converttopdf.ui.export.ExportViewModel
import com.nirv.converttopdf.ui.preview.PreviewViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ImageRepository() }
    single { PdfExporter(androidContext()) }
    single { ExportToPdfUseCase(get()) }

    viewModel { CaptureViewModel(get()) }
    viewModel { PreviewViewModel(get()) }
    viewModel { ExportViewModel(get(), get()) }
}
