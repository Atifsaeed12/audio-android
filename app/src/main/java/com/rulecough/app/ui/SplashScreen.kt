package com.rulecough.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rulecough.app.ui.theme.Viridis0
import com.rulecough.app.ui.theme.Viridis2
import com.rulecough.app.ui.theme.Viridis3
import com.rulecough.app.ui.theme.Viridis4
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(500))
        scale.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        delay(900)
        onDone()
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(Viridis0, Viridis2, Viridis3))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(112.dp).scale(scale.value)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(76.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Viridis3, Viridis4))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("RULe-Cough", color = Color.White, fontSize = 26.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Respiratory sound screening",
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}
