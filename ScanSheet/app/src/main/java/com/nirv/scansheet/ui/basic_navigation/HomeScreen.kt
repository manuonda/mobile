package com.nirv.scansheet.ui.basic_navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun HomeScreen(){

    Scaffold { paddingValues ->
        LazyColumn(Modifier.padding(paddingValues)) {
            items( 10) {
                Text("Soy la positioh  $it ")
            }
        }
    }

}