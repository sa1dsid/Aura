package com.aura.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

fun DrawScope.drawPlanet(
    bodyColor: Color,
    dotColor: Color,
    haloAlpha: Float = 0.15f,
) {
    val outerRadius = size.minDimension / 2f
    val bodyRadius = outerRadius * 0.80f
    val bodyCenter = center
    val lightSource = Offset(
        x = bodyCenter.x - bodyRadius * 0.5f,
        y = bodyCenter.y - bodyRadius * 0.55f,
    )

    if (haloAlpha > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    (bodyRadius / outerRadius) * 0.94f to Color.White.copy(alpha = haloAlpha),
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

    val step = bodyRadius / 6.5f
    val dotRadius = step * 0.2f
    var y = bodyCenter.y - bodyRadius + step / 2f
    while (y < bodyCenter.y + bodyRadius) {
        var x = bodyCenter.x - bodyRadius + step / 2f
        while (x < bodyCenter.x + bodyRadius) {
            val point = Offset(x, y)
            if ((point - bodyCenter).getDistance() <= bodyRadius - dotRadius) {
                val lit = 1f - ((point - lightSource).getDistance() / (bodyRadius * 2.2f))
                drawCircle(
                    color = dotColor.copy(alpha = (0.18f + 0.55f * lit).coerceIn(0f, 1f)),
                    radius = dotRadius,
                    center = point,
                )
            }
            x += step
        }
        y += step
    }
}
