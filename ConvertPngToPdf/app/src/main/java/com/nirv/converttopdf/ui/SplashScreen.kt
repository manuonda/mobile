package com.nirv.converttopdf.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nirv.converttopdf.ui.theme.PlazoMuted
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale   = remember { Animatable(0.7f) }
    var visible by remember { mutableStateOf(true) }

    val alpha by animateFloatAsState(
        targetValue    = if (visible) 1f else 0f,
        animationSpec  = tween(durationMillis = 350),
        label          = "splash_alpha",
        finishedListener = { if (!visible) onFinished() }
    )

    LaunchedEffect(Unit) {
        // Entrada con rebote
        scale.animateTo(
            targetValue   = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow
            )
        )
        delay(1000)
        // Salida con fade
        visible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier           = Modifier.size(48.dp),
                    tint               = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text       = "ConvertToPDF",
                fontWeight = FontWeight.Bold,
                fontSize   = 24.sp,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text      = "Escanea · Edita · Comparte",
                fontSize  = 13.sp,
                color     = PlazoMuted
            )
        }
    }
}
