package com.nirv.converttopdf.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.ui.theme.PlazoMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeHeader(documentCount: Int = 0) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        Text(
            text = "Mis documentos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(2.dp))
        val dateStr = SimpleDateFormat("EEEE, d MMMM", Locale("es")).format(Date())
            .replaceFirstChar { it.uppercase() }
        Text(
            text = if (documentCount > 0) "$documentCount documentos  •  $dateStr"
                   else dateStr,
            fontSize = 13.sp,
            color = PlazoMuted
        )
    }
}
