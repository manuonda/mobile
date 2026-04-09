package com.nirv.scansheet

import android.app.Application
import com.nirv.scansheet.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Punto de entrada de la app.
 * startKoin inicializa el contenedor de dependencias de Koin.
 * Debe declararse en AndroidManifest.xml como android:name=".ScanSheetApp"
 */
class ScanSheetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ScanSheetApp)
            modules(appModule)
        }
    }
}
