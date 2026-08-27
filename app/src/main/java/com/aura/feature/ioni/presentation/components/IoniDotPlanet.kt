package com.aura.feature.ioni.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

private const val GRID = 6

private const val STEP_RATIO = 0.11f

private const val RADIUS_BASE = 0.02526f

private const val RADIUS_FALLOFF = 0.03417f

private const val WAVE_MILLIS = 1400

private const val WAVE_LENGTH = 0.32f

private const val WAVE_DEPTH = 0.55f

private const val FADE_BASE = 1.0f

private const val FADE_FALLOFF = 1.718f

private val PlanetSize = 20.dp

@Composable
fun IoniDotPlanet(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = PlanetSize,
    alpha: Float = 1f,
    animated: Boolean = true,
    fade: Boolean = false,
) {
    val dots = remember { planetDots() }
    val phase = rememberWavePhase(animated)

    Canvas(modifier = modifier.size(size)) {
        val extent = this.size.minDimension
        val wave = phase.value

        dots.forEach { dot ->
            val base = if (fade) FADE_BASE - FADE_FALLOFF * dot.distance else 1f
            val pulse = if (animated) waveAlpha(dot.distance, wave) else 1f
            drawCircle(
                color = color,
                radius = dot.radius * extent,
                center = Offset(dot.x * extent, dot.y * extent),
                alpha = (alpha * base * pulse).coerceIn(0f, 1f),
            )
        }
    }
}

@Composable
private fun rememberWavePhase(animated: Boolean): State<Float> {
    if (!animated) return remember { mutableFloatStateOf(0f) }

    val transition = rememberInfiniteTransition(label = "planet-wave")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WAVE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "planet-wave-phase",
    )
}

private data class PlanetDot(
    val x: Float,
    val y: Float,
    val radius: Float,
    val distance: Float,
)

private fun planetDots(): List<PlanetDot> {
    val center = (GRID - 1) / 2f
    val dots = mutableListOf<PlanetDot>()

    for (row in 0 until GRID) {
        for (column in 0 until GRID) {
            val isCorner = (row == 0 || row == GRID - 1) && (column == 0 || column == GRID - 1)
            if (isCorner) continue

            val dx = (column - center) * STEP_RATIO
            val dy = (row - center) * STEP_RATIO
            val distance = hypot(dx, dy)

            dots += PlanetDot(
                x = 0.5f + dx,
                y = 0.5f + dy,
                radius = RADIUS_BASE - RADIUS_FALLOFF * distance,
                distance = distance,
            )
        }
    }

    return dots
}

private fun waveAlpha(distance: Float, phase: Float): Float {
    val offset = (distance / WAVE_LENGTH - phase) * 2f * PI.toFloat()
    return 1f - WAVE_DEPTH * (0.5f - 0.5f * sin(offset))
}
