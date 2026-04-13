package com.nirv.converttopdf.di

import com.nirv.converttopdf.data.ImageRepository
import com.nirv.converttopdf.data.PdfExporter
import com.nirv.converttopdf.data.SignatureRepository
import com.nirv.converttopdf.domain.usecase.ExportToPdfUseCase
import com.nirv.converttopdf.ui.capture.CaptureViewModel
import com.nirv.converttopdf.ui.export.ExportViewModel
import com.nirv.converttopdf.ui.preview.PreviewViewModel
import com.nirv.converttopdf.ui.signature.DrawSignatureViewModel
import com.nirv.converttopdf.ui.signature.SignatureViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Singles — una instancia compartida en toda la app
    single { ImageRepository() }
    single { SignatureRepository() }          // singleton compartido entre SignatureViewModel y DrawSignatureViewModel
    single { PdfExporter(androidContext()) }
    single { ExportToPdfUseCase(get()) }

    // ViewModels
    viewModel { CaptureViewModel(get()) }
    viewModel { PreviewViewModel(get(), get()) }
    viewModel { ExportViewModel(androidApplication(), get(), get()) }
    viewModel { SignatureViewModel(get(), get()) }      // ImageRepository + SignatureRepository
    viewModel { DrawSignatureViewModel(get()) }         // SignatureRepository
}
