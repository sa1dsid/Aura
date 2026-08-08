package com.aura.feature.home.presentation.components.mesh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.MeshCity
import com.aura.feature.home.domain.model.UserPresence
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object MeshMapDefaults {
    val Projection: WorldProjection =
        WorldProjection.Equirectangular(GeoBounds(north = 84.0, south = -84.0))

    const val DOT_COLUMNS = 116

    const val MIN_SCALE = 1f
    const val MAX_SCALE = 6f

    const val IDLE_DOT_RATIO = 0.40f
    const val LIVE_DOT_RATIO = 1.50f
    const val LIVE_GLOW_RATIO = 6.0f
    const val USER_DOT_RATIO = 1.60f
    const val USER_GLOW_RATIO = 6.5f
}

@Composable
fun MeshMap(
    cities: List<MeshCity>,
    userPresence: UserPresence?,
    modifier: Modifier = Modifier,
    projection: WorldProjection = MeshMapDefaults.Projection,
    state: MeshMapState = rememberMeshMapState(),
) {
    val colors = AuraTheme.colors
    val scope = rememberCoroutineScope()

    val idleDots = remember(projection) {
        WorldLandmass.dotGrid(projection, MeshMapDefaults.DOT_COLUMNS)
    }
    val liveDots = remember(cities, projection) {
        cities.filter { it.isLive }.map { projection.normalize(it.location) }
    }

    val pulse = rememberInfiniteTransition(label = "mesh-map").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "city-pulse",
    )

    var shownPresence by remember { mutableStateOf<UserPresence?>(null) }
    val presenceAlpha = remember { Animatable(0f) }
    LaunchedEffect(userPresence?.location) {
        if (shownPresence != null && shownPresence?.location != userPresence?.location) {
            presenceAlpha.animateTo(0f, tween(240))
        }
        shownPresence = userPresence
        if (userPresence != null) presenceAlpha.animateTo(1f, tween(420))
    }
    val userDot = remember(shownPresence, projection) {
        shownPresence?.let { projection.normalize(it.location) }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { state.onViewportChanged(it.toSize()) }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    state.onGesture(centroid, pan, zoom)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { scope.launch { state.animateToWorldView() } },
                )
            }
    ) {
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                }
                .drawWithCache {
                    val idlePoints = idleDots.scaledTo(size)
                    val livePoints = liveDots.scaledTo(size)
                    val userPoint = userDot?.scaledTo(size)

                    val spacing = size.width / MeshMapDefaults.DOT_COLUMNS
                    val idleDotPx = spacing * MeshMapDefaults.IDLE_DOT_RATIO
                    val liveDotPx = spacing * MeshMapDefaults.LIVE_DOT_RATIO
                    val liveGlowPx = spacing * MeshMapDefaults.LIVE_GLOW_RATIO
                    val userDotPx = spacing * MeshMapDefaults.USER_DOT_RATIO
                    val userGlowPx = spacing * MeshMapDefaults.USER_GLOW_RATIO

                    onDrawBehind {
                        drawPoints(
                            points = idlePoints,
                            pointMode = PointMode.Points,
                            color = colors.mapDotIdle,
                            strokeWidth = idleDotPx,
                            cap = StrokeCap.Round,
                        )

                        livePoints.forEachIndexed { index, point ->
                            drawGlowingDot(
                                center = point,
                                color = colors.mapDotLive,
                                coreRadius = liveDotPx / 2f,
                                glowRadius = liveGlowPx,
                                phase = pulse.value + index * 0.37f,
                            )
                        }

                        if (userPoint != null) {
                            drawUserDot(
                                center = userPoint,
                                color = colors.mapDotUser,
                                coreRadius = userDotPx / 2f,
                                glowRadius = userGlowPx,
                                phase = pulse.value,
                                alpha = presenceAlpha.value,
                            )
                        }
                    }
                }
        )
    }
}

private fun List<Offset>.scaledTo(size: Size): List<Offset> =
    map { Offset(it.x * size.width, it.y * size.height) }

private fun Offset.scaledTo(size: Size): Offset = Offset(x * size.width, y * size.height)

private fun breathe(phase: Float): Float = (sin(phase * 2f * PI.toFloat()) + 1f) / 2f

private fun DrawScope.drawGlowingDot(
    center: Offset,
    color: Color,
    coreRadius: Float,
    glowRadius: Float,
    phase: Float,
    alpha: Float = 1f,
) {
    val amplitude = (0.55f + 0.45f * breathe(phase)) * alpha
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.42f * amplitude),
                color.copy(alpha = 0.11f * amplitude),
                Color.Transparent,
            ),
            center = center,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
    )
    drawCircle(color = color.copy(alpha = alpha), radius = coreRadius, center = center)
}

private fun DrawScope.drawUserDot(
    center: Offset,
    color: Color,
    coreRadius: Float,
    glowRadius: Float,
    phase: Float,
    alpha: Float,
) {
    if (alpha <= 0f) return
    drawGlowingDot(
        center = center,
        color = color,
        coreRadius = coreRadius,
        glowRadius = glowRadius,
        phase = phase,
        alpha = alpha,
    )
    drawCircle(
        color = color.copy(alpha = 0.35f * alpha),
        radius = coreRadius * 2.4f,
        center = center,
        style = Stroke(width = coreRadius * 0.5f),
    )
}
