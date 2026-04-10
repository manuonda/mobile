package com.nirv.converttopdf

import android.app.Application
import com.nirv.converttopdf.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ConvertPdfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ConvertPdfApp)
            modules(appModule)
        }
    }
}
