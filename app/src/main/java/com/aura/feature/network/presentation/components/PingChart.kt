package com.aura.feature.network.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.auraGlowLayer
import com.aura.core.designsystem.component.drawAuraGlow
import com.aura.core.designsystem.theme.AuraColors
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.domain.model.PING_GOOD_MS
import com.aura.feature.network.domain.model.PING_IDEAL_MS
import com.aura.feature.network.domain.model.PING_WARNING_MS
import com.aura.feature.network.domain.model.PingRecord

private const val AXIS_MAX_MS = 70f

private val PlotHeight = 207.dp

private val TopGridline = 16.5.dp

private val BottomGridline = 200.5.dp

private val TopLabelInset = 10.dp

private val BottomLabelInset = 194.dp

private val LatestBadgeInset = 157.dp

private val TopLabelWidth = 30.dp

private val BottomLabelWidth = 24.dp

private val AxisGap = 19.dp

private val GridlineWidth = 1.25.dp

private val ThresholdWidth = 0.5.dp

private val DashLength = 3.5.dp

private val LineWidth = 1.25.dp

private val PointRadius = 1.33.dp

private val VpnRingRadius = 2.5.dp

private val VpnRingWidth = 1.dp

private val LatestRadius = 4.dp

@Composable
fun PingChart(
    history: List<PingRecord>,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(PlotHeight)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val top = TopGridline.toPx()
                val bottom = BottomGridline.toPx()

                drawGridline(colors.border, top, TopLabelWidth.toPx())
                drawGridline(colors.border, bottom, BottomLabelWidth.toPx())

                drawThreshold(PING_WARNING_MS, top, bottom, colors.warning.copy(alpha = 0.5f))
                drawThreshold(PING_GOOD_MS, top, bottom, colors.accentBlue.copy(alpha = 0.5f))
                drawThreshold(PING_IDEAL_MS, top, bottom, colors.green)

                if (history.isEmpty()) return@Canvas

                val points = history.toPoints(size.width, top, bottom)
                drawArea(points, bottom, colors.iceBlue)
                drawTrend(points, colors.accentBlue)
                drawPoints(history, points, colors)
            }

            AxisValue(
                text = stringResource(R.string.net_axis_top),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = TopLabelInset),
            )

            AxisValue(
                text = stringResource(R.string.net_axis_bottom),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = BottomLabelInset),
            )

            history.lastOrNull()?.let { latest ->
                LatestBadge(
                    record = latest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = LatestBadgeInset),
                )
            }
        }

        Spacer(Modifier.height(AxisGap))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AxisValue(stringResource(R.string.net_axis_first), Modifier.weight(1f), TextAlign.Start)
            AxisValue(stringResource(R.string.net_axis_mid), Modifier.weight(1f), TextAlign.Center)
            AxisValue(stringResource(R.string.net_axis_last), Modifier.weight(1f), TextAlign.End)
        }
    }
}

@Composable
private fun AxisValue(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        style = AuraTheme.typography.caption,
        color = AuraTheme.colors.textSecondary,
        textAlign = align,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun LatestBadge(record: PingRecord, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .auraDropShadow(
                color = colors.glowIce.copy(alpha = 0.60f),
                blurRadius = 4.dp,
                cornerRadius = 10.dp,
            )
            .clip(RoundedCornerShape(10.dp))
            .background(colors.background)
            .border(0.5.dp, colors.textBright, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.net_latest),
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.net_ping_value, record.pingMs),
            style = AuraTheme.typography.latestValue,
            color = if (record.vpnActive) colors.warning else colors.textBright,
        )
    }
}

private fun List<PingRecord>.toPoints(width: Float, top: Float, bottom: Float): List<Offset> {
    val span = bottom - top
    return mapIndexed { index, record ->
        val x = if (size == 1) width / 2f else width * index / (size - 1)
        val fraction = (record.pingMs / AXIS_MAX_MS).coerceIn(0f, 1f)
        Offset(x, bottom - span * fraction)
    }
}

private fun DrawScope.drawGridline(color: Color, y: Float, startX: Float) {
    drawLine(
        color = color,
        start = Offset(startX, y),
        end = Offset(size.width, y),
        strokeWidth = GridlineWidth.toPx(),
    )
}

private fun DrawScope.drawThreshold(valueMs: Int, top: Float, bottom: Float, color: Color) {
    val y = bottom - (bottom - top) * (valueMs / AXIS_MAX_MS)
    val dash = DashLength.toPx()
    val stroke = ThresholdWidth.toPx()

    var x = 0f
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(minOf(x + dash, size.width), y),
            strokeWidth = stroke,
        )
        x += dash * 2f
    }
}

private fun DrawScope.drawArea(points: List<Offset>, bottom: Float, color: Color) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, bottom)
        points.forEach { lineTo(it.x, it.y) }
        lineTo(points.last().x, bottom)
        close()
    }

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.22f), Color.Transparent),
            startY = points.minOf { it.y },
            endY = bottom,
        ),
    )
}

private fun DrawScope.drawTrend(points: List<Offset>, color: Color) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawPoints(
    history: List<PingRecord>,
    points: List<Offset>,
    colors: AuraColors,
) {
    points.forEachIndexed { index, point ->
        drawCircle(color = colors.accentBlue, radius = PointRadius.toPx(), center = point)

        if (history[index].vpnActive) {
            drawCircle(
                color = colors.warning,
                radius = VpnRingRadius.toPx(),
                center = point,
                style = Stroke(width = VpnRingWidth.toPx()),
            )
        }
    }

    val last = points.lastOrNull() ?: return
    val glow = auraGlowLayer(
        shadow = AuraShadow(colors.glowIce, 6.dp),
        width = LatestRadius.toPx() * 2f,
        height = LatestRadius.toPx() * 2f,
        center = Offset.Zero,
    )

    drawAuraGlow(glow, center = last)
    drawCircle(color = colors.textBright, radius = LatestRadius.toPx(), center = last)
}
