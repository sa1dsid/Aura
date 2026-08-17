package com.aura.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme

private val ScrollBarWidth = 16.dp

private val ThumbWidth = 12.dp

private val ArrowSize = 8.dp

@Composable
fun AuraLogScrollBar(
    fraction: Float,
    visibleFraction: Float,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .width(ScrollBarWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(bottomEnd = 16.dp))
            .background(colors.textBright)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScrollArrow(pointsUp = true)

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            val thumbHeight = maxHeight * visibleFraction.coerceIn(0.15f, 1f)
            val offset = (maxHeight - thumbHeight) * fraction.coerceIn(0f, 1f)

            Box(
                Modifier
                    .padding(top = offset)
                    .width(ThumbWidth)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(100.dp))
                    .background(colors.textDisabled)
            )
        }

        ScrollArrow(pointsUp = false)
    }
}

@Composable
private fun ScrollArrow(pointsUp: Boolean, modifier: Modifier = Modifier) {
    val color = AuraTheme.colors.textDisabled

    Canvas(
        modifier = modifier
            .width(ThumbWidth)
            .height(ArrowSize)
    ) {
        val path = Path().apply {
            if (pointsUp) {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(size.width / 2f, size.height)
                lineTo(size.width, 0f)
                lineTo(0f, 0f)
            }
            close()
        }
        drawPath(path = path, color = color)
    }
}

fun ScrollState.scrollProgress(): Float =
    if (maxValue == 0) 0f else value.toFloat() / maxValue

fun ScrollState.visibleFraction(): Float {
    val content = viewportSize + maxValue
    return if (content == 0) 1f else viewportSize.toFloat() / content
}
