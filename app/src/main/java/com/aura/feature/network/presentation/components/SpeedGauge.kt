package com.aura.feature.network.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.auraGlowLayer
import com.aura.core.designsystem.component.drawAuraGlow
import com.aura.core.designsystem.theme.AuraTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TICK_COUNT = 62

private const val START_ANGLE = -76.5f

private const val END_ANGLE = 78.5f

private val GaugeHeight = 341.dp

private val GaugeRadius = 170.5.dp

private val VisibleInset = 26.dp

private val TickLength = 16.5.dp

private val TickWidth = 1.dp

private val MarkWidth = 3.dp

@Composable
fun SpeedGauge(
    litFraction: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AuraTheme.colors

    val lit by animateFloatAsState(
        targetValue = litFraction.coerceIn(0f, 1f),
        animationSpec = tween(220),
        label = "gauge-lit",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GaugeHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = GaugeRadius.toPx()
            val center = Offset(size.width - VisibleInset.toPx() - radius, size.height / 2f)
            val litTicks = (TICK_COUNT * lit).roundToInt()

            val markGlow = auraGlowLayer(
                shadow = AuraShadow(colors.glowIce.copy(alpha = 0.80f), 8.dp),
                width = MarkWidth.toPx(),
                height = TickLength.toPx(),
                center = Offset.Zero,
            )

            repeat(TICK_COUNT) { index ->
                val angle = START_ANGLE + (END_ANGLE - START_ANGLE) * index / (TICK_COUNT - 1)
                val isMark = index == 0
                val isLit = index < litTicks

                drawTick(
                    center = center,
                    radius = radius,
                    angleDegrees = angle,
                    color = when {
                        isMark -> colors.textBright
                        isLit -> colors.textBright
                        else -> colors.textDisabled
                    },
                    width = if (isMark) MarkWidth.toPx() else TickWidth.toPx(),
                    length = TickLength.toPx(),
                )

                if (isMark) {
                    val midpoint = radius - TickLength.toPx() / 2f
                    val radians = Math.toRadians(angle.toDouble())
                    drawAuraGlow(
                        layer = markGlow,
                        center = Offset(
                            center.x + cos(radians).toFloat() * midpoint,
                            center.y + sin(radians).toFloat() * midpoint,
                        ),
                    )
                }
            }
        }

        content()
    }
}

private fun DrawScope.drawTick(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    color: Color,
    width: Float,
    length: Float,
) {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    val inner = radius - length

    drawLine(
        color = color,
        start = Offset(center.x + dx * inner, center.y + dy * inner),
        end = Offset(center.x + dx * radius, center.y + dy * radius),
        strokeWidth = width,
        cap = StrokeCap.Butt,
    )
}
