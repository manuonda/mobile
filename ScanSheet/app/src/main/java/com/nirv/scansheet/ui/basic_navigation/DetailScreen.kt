package com.nirv.scansheet.ui.basic_navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun DetailScreen(id:String){
    Scaffold() { paddingValues ->
        Box(modifier = Modifier.padding((paddingValues))) {
            Text(text = "Detail $id", fontSize = 25.sp)
        }

    }
}