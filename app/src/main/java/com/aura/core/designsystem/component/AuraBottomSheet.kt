package com.aura.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

private val GrabberShape = RoundedCornerShape(percent = 50)

private val MIN_GAP_ABOVE_NAVIGATION_BAR = 12.dp

private const val ENTER_MILLIS = 250

private const val EXIT_MILLIS = 200

private const val SCRIM_ALPHA = 0.70f

private const val DISMISS_TRAVEL_FRACTION = 0.4f

private const val DISMISS_VELOCITY = 900f

@Composable
fun AuraBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 24.dp,
    bottomGap: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AuraTheme.colors
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(0f) }
    val inspecting = LocalInspectionMode.current

    var rendered by remember { mutableStateOf(inspecting && visible) }
    var entered by remember { mutableStateOf(inspecting && visible) }
    var sheetHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(visible) {
        if (visible) {
            rendered = true
            return@LaunchedEffect
        }
        if (!rendered) return@LaunchedEffect
        if (sheetHeight > 0) {
            slide.animateTo(
                targetValue = sheetHeight.toFloat(),
                animationSpec = tween(EXIT_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        rendered = false
        entered = false
        sheetHeight = 0
    }

    LaunchedEffect(visible, sheetHeight) {
        if (inspecting || !visible || sheetHeight <= 0) return@LaunchedEffect
        if (!entered) {
            slide.snapTo(sheetHeight.toFloat())
            entered = true
        }
        if (slide.value != 0f) {
            slide.animateTo(0f, tween(ENTER_MILLIS, easing = LinearOutSlowInEasing))
        }
    }

    if (!rendered) return

    BackHandler(enabled = visible, onBack = onDismissRequest)

    val travel = if (sheetHeight > 0) slide.value / sheetHeight else 1f
    val scrimAlpha = if (entered) SCRIM_ALPHA * (1f - travel).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = 0, y = slide.value.roundToInt()) }
                .graphicsLayer { alpha = if (entered) 1f else 0f }
                .onSizeChanged { sheetHeight = it.height }
                .clip(SheetShape)
                .background(Brush.verticalGradient(listOf(colors.sheetTop, colors.sheetBottom)))
                .border(0.5.dp, colors.sheetBorder, SheetShape)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.15f),
                            1f to Color.Transparent,
                        ),
                        size = size.copy(height = 1.dp.toPx()),
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch { slide.snapTo((slide.value + delta).coerceAtLeast(0f)) }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        val height = sheetHeight.toFloat()
                        val dismissed = height > 0f &&
                            (slide.value > height * DISMISS_TRAVEL_FRACTION || velocity > DISMISS_VELOCITY)
                        if (dismissed) {
                            onDismissRequest()
                        } else {
                            slide.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            )
                        }
                    },
                )
                .padding(top = 10.dp)
                .padding(horizontal = horizontalPadding)
                .padding(bottom = sheetBottomGap(bottomGap)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(GrabberShape)
                    .background(colors.sheetMuted)
            )

            Spacer(Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun sheetBottomGap(designGap: Dp): Dp {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return maxOf(designGap, navigationBar + MIN_GAP_ABOVE_NAVIGATION_BAR)
}
