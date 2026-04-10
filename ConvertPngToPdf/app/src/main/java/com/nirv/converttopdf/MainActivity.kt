package com.nirv.converttopdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nirv.converttopdf.navigation.AppNavHost
import com.nirv.converttopdf.ui.theme.ConvertPngToPdfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConvertPngToPdfTheme {
                AppNavHost()
            }
        }
    }
}
