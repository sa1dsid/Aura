package com.aura.feature.account.presentation.menu.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme

private val AvatarSize = 48.dp

private const val BODY_FRACTION = 21.5f / 24f

private const val RING_FRACTION = 21f / 24f

private val DotPitchX = 5.49.dp

private val DotPitchY = 3.98.dp

private val DotSize = 0.78.dp

@Composable
fun AccountAvatar(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Box(
        modifier
            .size(AvatarSize)
            .drawBehind {
                drawAccountAvatar(
                    bodyColor = colors.surfaceTop,
                    dotColor = colors.accountAvatarDot,
                    ringColor = colors.border,
                )
            }
    )
}

private fun DrawScope.drawAccountAvatar(
    bodyColor: Color,
    dotColor: Color,
    ringColor: Color,
) {
    val outerRadius = size.minDimension / 2f
    val bodyRadius = outerRadius * BODY_FRACTION

    drawCircle(color = bodyColor, radius = bodyRadius, center = center)

    val pitchX = DotPitchX.toPx()
    val pitchY = DotPitchY.toPx()
    val dot = DotSize.toPx()
    val reach = bodyRadius - dot

    var y = center.y - bodyRadius + pitchY / 2f
    while (y <= center.y + bodyRadius) {
        var x = center.x - bodyRadius + pitchX / 2f
        while (x <= center.x + bodyRadius) {
            val point = Offset(x, y)
            if ((point - center).getDistance() <= reach) {
                drawRoundRect(
                    color = dotColor,
                    topLeft = Offset(x - dot / 2f, y - dot / 2f),
                    size = Size(dot, dot),
                    cornerRadius = CornerRadius(dot / 2f),
                )
            }
            x += pitchX
        }
        y += pitchY
    }

    val ringRadius = outerRadius * RING_FRACTION
    drawCircle(
        color = ringColor,
        radius = ringRadius,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )
}
