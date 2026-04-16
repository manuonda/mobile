package com.nirv.converttopdf.di

import com.nirv.converttopdf.data.ImageRepository
import com.nirv.converttopdf.data.PdfExporter
import com.nirv.converttopdf.data.ReadPdfRepository
import com.nirv.converttopdf.data.SignatureRepository
import com.nirv.converttopdf.data.db.AppDatabase
import com.nirv.converttopdf.data.repository.DocumentRepository
import com.nirv.converttopdf.domain.usecase.ExportToPdfUseCase
import com.nirv.converttopdf.ui.capture.CaptureViewModel
import com.nirv.converttopdf.ui.export.ExportViewModel
import com.nirv.converttopdf.ui.files.DirectoryViewModel
import com.nirv.converttopdf.ui.preview.PreviewViewModel
import com.nirv.converttopdf.ui.signature.DrawSignatureViewModel
import com.nirv.converttopdf.ui.signature.SignatureViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // ── Database ──────────────────────────────────────────────────────────────
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().documentDao() }
    single { DocumentRepository(get(), androidContext()) }

    // ── Singles ───────────────────────────────────────────────────────────────
    single { ImageRepository() }
    single { SignatureRepository() }
    single { PdfExporter(androidContext()) }
    single { ExportToPdfUseCase(get()) }
    single { ReadPdfRepository(get()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { CaptureViewModel(get()) }                                    // DocumentRepository
    viewModel { (docId: Long) -> PreviewViewModel(docId, get(), get(), get()) } // docId + DocumentRepository + ImageRepository + ExportToPdfUseCase
    viewModel { ExportViewModel(androidApplication(), get(), get()) }
    viewModel { SignatureViewModel(get(), get()) }
    viewModel { DrawSignatureViewModel(get()) }
    viewModel { DirectoryViewModel(get()) }
}
