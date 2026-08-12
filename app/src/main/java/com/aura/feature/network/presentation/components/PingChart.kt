package com.aura.feature.network.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.auraGlowLayer
import com.aura.core.designsystem.component.drawAuraGlow
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.domain.model.PING_GOOD_MS
import com.aura.feature.network.domain.model.PING_IDEAL_MS
import com.aura.feature.network.domain.model.PING_WARNING_MS
import com.aura.feature.network.domain.model.PingRecord
import kotlin.math.max

private val LineWidth = 1.5.dp

private val DotRadius = 2.5.dp

private val VpnDotRadius = 3.5.dp

private val VpnDotStroke = 1.dp

private val ThresholdWidth = 0.5.dp

private const val HEADROOM_MS = 10f

private const val MIN_SPAN_MS = 20f

@Composable
fun PingChart(
    history: List<PingRecord>,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            if (history.isEmpty()) return@Canvas

            val range = history.pingRange()
            val points = history.mapIndexed { index, record ->
                Offset(
                    x = if (history.size == 1) size.width / 2f
                    else size.width * index / (history.size - 1),
                    y = range.yOf(record.pingMs.toFloat(), size.height),
                )
            }

            drawThreshold(PING_IDEAL_MS, range, colors.mint.copy(alpha = 0.35f))
            drawThreshold(PING_GOOD_MS, range, colors.accentBlue.copy(alpha = 0.30f))
            drawThreshold(PING_WARNING_MS, range, colors.danger.copy(alpha = 0.30f))

            drawAreaUnder(points, colors.accentBlue)
            drawLine(points, colors.accentBlue)
            drawDots(history, points, colors.accentBlue, colors.danger, colors.background)
        }
    }
}

private data class PingRange(val min: Float, val max: Float) {
    fun yOf(value: Float, height: Float): Float {
        val span = (max - min).coerceAtLeast(1f)
        return height - (value - min) / span * height
    }
}

private fun List<PingRecord>.pingRange(): PingRange {
    val values = map { it.pingMs.toFloat() }
    val lowest = max(0f, (values.min() - HEADROOM_MS))
    val highest = max(values.max() + HEADROOM_MS, lowest + MIN_SPAN_MS)
    return PingRange(min = lowest, max = highest)
}

private fun DrawScope.drawThreshold(valueMs: Int, range: PingRange, color: Color) {
    val value = valueMs.toFloat()
    if (value < range.min || value > range.max) return

    val y = range.yOf(value, size.height)
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = ThresholdWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
    )
}

private fun DrawScope.drawAreaUnder(points: List<Offset>, color: Color) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, size.height)
        points.forEach { lineTo(it.x, it.y) }
        lineTo(points.last().x, size.height)
        close()
    }

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            listOf(color.copy(alpha = 0.16f), Color.Transparent),
        ),
    )
}

private fun DrawScope.drawLine(points: List<Offset>, color: Color) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawDots(
    history: List<PingRecord>,
    points: List<Offset>,
    color: Color,
    vpnColor: Color,
    holeColor: Color,
) {
    val latest = points.lastIndex
    val glow = auraGlowLayer(
        shadow = AuraShadow(color.copy(alpha = 0.60f), 8.dp),
        width = DotRadius.toPx() * 2f,
        height = DotRadius.toPx() * 2f,
        center = Offset.Zero,
    )

    points.forEachIndexed { index, point ->
        val record = history[index]

        if (index == latest) drawAuraGlow(glow, center = point)

        if (record.vpnActive) {
            drawCircle(color = holeColor, radius = VpnDotRadius.toPx(), center = point)
            drawCircle(
                color = vpnColor,
                radius = VpnDotRadius.toPx(),
                center = point,
                style = Stroke(width = VpnDotStroke.toPx()),
            )
        } else {
            drawCircle(color = color, radius = DotRadius.toPx(), center = point)
        }
    }
}
