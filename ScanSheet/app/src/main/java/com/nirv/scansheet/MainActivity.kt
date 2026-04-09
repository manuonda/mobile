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
            ScanSheetTheme() {
                AppNavHost()
            }
        }
    }
}
