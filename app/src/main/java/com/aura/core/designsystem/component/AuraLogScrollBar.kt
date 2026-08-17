package com.aura.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme

private val ScrollBarWidth = 16.dp

private val ThumbWidth = 12.dp

private val ArrowSize = 8.dp

private const val MIN_THUMB_FRACTION = 0.15f

@Composable
fun AuraLogScrollBar(
    fraction: () -> Float,
    visibleFraction: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val thumbColor = colors.textDisabled

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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .drawBehind {
                    val track = size.height
                    if (track <= 0f) return@drawBehind

                    val thumbWidth = ThumbWidth.toPx()
                    val thumbHeight =
                        track * visibleFraction().coerceIn(MIN_THUMB_FRACTION, 1f)
                    val top = (track - thumbHeight) * fraction().coerceIn(0f, 1f)
                    val corner = minOf(thumbWidth, thumbHeight) / 2f

                    drawRoundRect(
                        color = thumbColor,
                        topLeft = Offset((size.width - thumbWidth) / 2f, top),
                        size = Size(thumbWidth, thumbHeight),
                        cornerRadius = CornerRadius(corner),
                    )
                }
        )

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

fun LazyListState.scrollProgress(): Float {
    val info = layoutInfo
    val total = info.totalItemsCount
    val visible = info.visibleItemsInfo
    if (total == 0 || visible.isEmpty()) return 0f

    val scrollable = total - visible.size
    if (scrollable <= 0) return 0f

    val first = visible.first()
    val within = if (first.size == 0) 0f else (-first.offset).toFloat() / first.size
    return ((firstVisibleItemIndex + within) / scrollable).coerceIn(0f, 1f)
}

fun LazyListState.visibleFraction(): Float {
    val info = layoutInfo
    val total = info.totalItemsCount
    val visible = info.visibleItemsInfo.size
    if (total == 0 || visible == 0) return 1f
    return (visible.toFloat() / total).coerceIn(0f, 1f)
}
