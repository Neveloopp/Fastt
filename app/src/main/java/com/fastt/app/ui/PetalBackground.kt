package com.fastt.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class Petal(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val sway: Float,
    val phase: Float,
)

@Composable
fun PetalBackground(modifier: Modifier = Modifier, petalsCount: Int = 26) {
    val petals = remember {
        List(petalsCount) {
            Petal(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = 6f + Random.nextFloat() * 10f,
                speed = 0.15f + Random.nextFloat() * 0.35f,
                sway = 0.02f + Random.nextFloat() * 0.06f,
                phase = Random.nextFloat() * 6.28f,
            )
        }
    }

    val inf = rememberInfiniteTransition(label = "petals")
    val t = inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(9000, easing = LinearEasing)),
        label = "t"
    ).value

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        petals.forEachIndexed { idx, p ->
            val yy = ((p.y + t * p.speed) % 1f) * h
            val xx = (p.x + kotlin.math.sin(p.phase + t * 6.28f + idx) * p.sway) * w
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = p.size,
                center = Offset(xx, yy)
            )
        }
    }
}
