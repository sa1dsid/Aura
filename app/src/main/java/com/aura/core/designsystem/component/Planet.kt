package com.aura.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

private const val DOT_PITCH_X_FRACTION = 0.1272f

private const val DOT_PITCH_Y_FRACTION = 0.0938f

private val DotSize = 0.78.dp

fun DrawScope.drawPlanet(
    bodyColor: Color,
    dotColor: Color,
    bodyFraction: Float = 0.5f,
    haloAlpha: Float = 0.52f,
) {
    val outerRadius = size.minDimension / 2f
    val bodyRadius = outerRadius * bodyFraction
    val bodyCenter = center

    if (haloAlpha > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    bodyFraction * 0.92f to Color.White.copy(alpha = haloAlpha),
                    bodyFraction + (1f - bodyFraction) * 0.35f to
                        Color.White.copy(alpha = haloAlpha * 0.55f),
                    bodyFraction + (1f - bodyFraction) * 0.7f to
                        Color.White.copy(alpha = haloAlpha * 0.14f),
                    1f to Color.Transparent,
                ),
                center = bodyCenter,
                radius = outerRadius,
            ),
            radius = outerRadius,
            center = bodyCenter,
        )
    }

    drawCircle(color = bodyColor, radius = bodyRadius, center = bodyCenter)

    val pitchX = bodyRadius * 2f * DOT_PITCH_X_FRACTION
    val pitchY = bodyRadius * 2f * DOT_PITCH_Y_FRACTION
    val dotRadius = DotSize.toPx() / 2f
    var y = bodyCenter.y - bodyRadius + pitchY / 2f
    while (y < bodyCenter.y + bodyRadius) {
        var x = bodyCenter.x - bodyRadius + pitchX / 2f
        while (x < bodyCenter.x + bodyRadius) {
            val point = Offset(x, y)
            if ((point - bodyCenter).getDistance() <= bodyRadius - dotRadius) {
                drawCircle(color = dotColor, radius = dotRadius, center = point)
            }
            x += pitchX
        }
        y += pitchY
    }
}
