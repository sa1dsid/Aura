package com.aura.feature.network.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.auraArcGlow
import com.aura.core.designsystem.component.drawAuraArcGlow
import com.aura.core.designsystem.theme.AuraTheme

private const val START_ANGLE = 135f

private const val SWEEP_ANGLE = 270f

private val GaugeStroke = 6.dp

@Composable
fun SpeedGauge(
    progress: Float,
    accentColor: Color,
    diameter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AuraTheme.colors

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(220),
        label = "gauge-progress",
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .gaugeGraphics(
                    fraction = { animatedProgress },
                    trackColor = colors.border,
                    arcColor = accentColor,
                    glowShadows = listOf(
                        AuraShadow(accentColor.copy(alpha = 0.80f), 6.dp),
                        AuraShadow(accentColor.copy(alpha = 0.40f), 18.dp, 2.dp),
                    ),
                )
        )
        content()
    }
}

private fun Modifier.gaugeGraphics(
    fraction: () -> Float,
    trackColor: Color,
    arcColor: Color,
    glowShadows: List<AuraShadow>,
): Modifier = drawWithCache {
    val strokePx = GaugeStroke.toPx()
    val topLeft = Offset(strokePx / 2f, strokePx / 2f)
    val arcSize = Size(size.width - strokePx, size.height - strokePx)
    val glows = glowShadows.map { auraArcGlow(it, strokePx) }

    onDrawBehind {
        drawArc(
            color = trackColor,
            startAngle = START_ANGLE,
            sweepAngle = SWEEP_ANGLE,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        val sweep = SWEEP_ANGLE * fraction()
        if (sweep <= 0f) return@onDrawBehind

        glows.forEach { glow ->
            drawAuraArcGlow(glow, topLeft, arcSize, START_ANGLE, sweep)
        }

        drawArc(
            color = arcColor,
            startAngle = START_ANGLE,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}
