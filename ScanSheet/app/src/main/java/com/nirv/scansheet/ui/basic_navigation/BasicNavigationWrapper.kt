package com.nirv.scansheet.ui.basic_navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

//
data object Home
data class  Detail(val id:String)  //pasamos parametros class

@Composable
fun BasicNavigationWrapper(){
    val backStack = remember { mutableStateListOf<Any>() }

    NavDisplay(
        backStack = backStack,
        onBack = {backStack.removeLastOrNull()},
        entryProvider =  { key ->
            when(key) {
                is Home -> NavEntry(key = Home) {
                    HomeScreen()
                }
                is Detail -> NavEntry(key = Detail(key.id)) {
                    DetailScreen($key)
                }

                else -> NavEntry(key = Unit) {
                    Text(text = "Error")
                }
            }
        }
    )
}
