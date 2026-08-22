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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

private val DrawerShape = RoundedCornerShape(16.dp)

private const val ENTER_MILLIS = 250

private const val EXIT_MILLIS = 200

private const val SCRIM_ALPHA = 0.70f

private const val DISMISS_TRAVEL_FRACTION = 0.4f

private const val DISMISS_VELOCITY = -900f

@Composable
fun AuraTopDrawer(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AuraTheme.colors
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(0f) }
    val inspecting = LocalInspectionMode.current

    var rendered by remember { mutableStateOf(inspecting && visible) }
    var entered by remember { mutableStateOf(inspecting && visible) }
    var drawerHeight by remember { mutableIntStateOf(0) }

    val currentHeight by rememberUpdatedState(drawerHeight)
    val currentDismiss by rememberUpdatedState(onDismissRequest)

    suspend fun settle(velocity: Float) {
        val height = currentHeight.toFloat()
        val dismissed = height > 0f &&
            (slide.value > height * DISMISS_TRAVEL_FRACTION || velocity < DISMISS_VELOCITY)
        if (dismissed) {
            currentDismiss()
        } else {
            slide.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    val nestedScroll = rememberDrawerNestedScroll(slide, scope, ::settle)

    LaunchedEffect(visible) {
        if (visible) {
            rendered = true
            return@LaunchedEffect
        }
        if (!rendered) return@LaunchedEffect
        if (drawerHeight > 0) {
            slide.animateTo(
                targetValue = drawerHeight.toFloat(),
                animationSpec = tween(EXIT_MILLIS, easing = FastOutLinearInEasing),
            )
        }
        rendered = false
        entered = false
        drawerHeight = 0
    }

    LaunchedEffect(visible, drawerHeight) {
        if (inspecting || !visible || drawerHeight <= 0) return@LaunchedEffect
        if (!entered) {
            slide.snapTo(drawerHeight.toFloat())
            entered = true
        }
        if (slide.value != 0f) {
            slide.animateTo(0f, tween(ENTER_MILLIS, easing = LinearOutSlowInEasing))
        }
    }

    if (!rendered) return

    BackHandler(enabled = visible, onBack = onDismissRequest)

    val travel = if (drawerHeight > 0) slide.value / drawerHeight else 1f
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
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = 0, y = -slide.value.roundToInt()) }
                .graphicsLayer { alpha = if (entered) 1f else 0f }
                .onSizeChanged { drawerHeight = it.height }
                .clip(DrawerShape)
                .background(colors.background)
                .border(1.dp, colors.border, DrawerShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .nestedScroll(nestedScroll)
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch { slide.snapTo((slide.value - delta).coerceAtLeast(0f)) }
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> settle(velocity) },
                )
                .statusBarsPadding(),
            content = content,
        )
    }
}

@Composable
private fun rememberDrawerNestedScroll(
    slide: Animatable<Float, *>,
    scope: CoroutineScope,
    settle: suspend (Float) -> Unit,
): NestedScrollConnection = remember(slide, scope) {
    object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            if (delta <= 0f || slide.value <= 0f) return Offset.Zero

            val consumed = min(delta, slide.value)
            scope.launch { slide.snapTo(slide.value - consumed) }
            return Offset(x = 0f, y = consumed)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val delta = available.y
            if (delta >= 0f) return Offset.Zero

            scope.launch { slide.snapTo(slide.value - delta) }
            return Offset(x = 0f, y = delta)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (slide.value <= 0f) return Velocity.Zero

            settle(available.y)
            return available
        }
    }
}
