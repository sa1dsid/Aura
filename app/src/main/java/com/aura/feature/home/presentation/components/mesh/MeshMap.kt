package com.aura.feature.home.presentation.components.mesh

import android.graphics.Paint as AndroidPaint
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.aura.core.designsystem.component.AuraGlowLayer
import com.aura.core.designsystem.component.auraGlowLayer
import com.aura.core.designsystem.component.brightDotShadows
import com.aura.core.designsystem.component.drawAuraGlow
import com.aura.core.designsystem.component.signalDotShadows
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.MeshCity
import com.aura.feature.home.domain.model.UserPresence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

object MeshMapDefaults {
    val AssetBounds: GeoBounds = GeoBounds(
        north = 83.792,
        south = -89.958,
        west = -170.099,
        east = 190.261,
    )

    const val ASSET_WIDTH_DP = 313f
    const val ASSET_HEIGHT_DP = 153f

    val Projection: WorldProjection = WorldProjection(
        bounds = AssetBounds,
        aspectRatio = ASSET_WIDTH_DP / ASSET_HEIGHT_DP,
    )

    const val DOT_COLUMNS = 65

    const val MIN_SCALE = 1f
    const val MAX_SCALE = 6f

    const val IDLE_DOT_RATIO = 0.243f
    const val LIVE_DOT_RATIO = 0.83f

    const val USER_DOT_RATIO = 1.66f

    const val PRESENCE_FADE_MILLIS = 300
}

private const val GLOW_FLOOR = 0.55f

private const val GLOW_SWING = 0.45f

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

    val liveShadows = remember(colors) { colors.brightDotShadows }
    val userShadows = remember(colors) { colors.signalDotShadows }

    val context = LocalContext.current
    val dotGrid by produceState(
        initialValue = WorldLandmass.cached(projection, MeshMapDefaults.DOT_COLUMNS),
        projection,
        context,
    ) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                WorldLandmass.dotGrid(context, projection, MeshMapDefaults.DOT_COLUMNS)
            }
        }
    }
    val liveDots = remember(cities, dotGrid) {
        val grid = dotGrid ?: return@remember emptyList()
        cities.filter { it.isLive }.map { grid.snap(projection.normalize(it.location)) }
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
            presenceAlpha.animateTo(0f, tween(MeshMapDefaults.PRESENCE_FADE_MILLIS))
        }
        shownPresence = userPresence
        if (userPresence != null) {
            presenceAlpha.animateTo(1f, tween(MeshMapDefaults.PRESENCE_FADE_MILLIS))
        }
    }
    val userDot = remember(shownPresence, dotGrid) {
        val grid = dotGrid ?: return@remember null
        shownPresence?.let { grid.snap(projection.normalize(it.location)) }
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
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                }
        ) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val grid = dotGrid ?: return@drawWithCache onDrawBehind {}

                        val spacing = size.width / MeshMapDefaults.DOT_COLUMNS
                        val coordinates = grid.dots.toPointArray(size)
                        val paint = pointPaint(
                            colors.mapDotIdle,
                            spacing * MeshMapDefaults.IDLE_DOT_RATIO,
                        )

                        onDrawBehind {
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawPoints(coordinates, paint)
                            }
                        }
                    }
            )

            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer()
                    .drawWithCache {
                        val livePoints = liveDots.scaledTo(size)
                        val userPoint = userDot?.scaledTo(size)

                        val spacing = size.width / MeshMapDefaults.DOT_COLUMNS
                        val liveDotPx = spacing * MeshMapDefaults.LIVE_DOT_RATIO
                        val userDotPx = spacing * MeshMapDefaults.USER_DOT_RATIO

                        val liveGlow = liveShadows.map {
                            auraGlowLayer(it, liveDotPx, liveDotPx, Offset.Zero)
                        }
                        val userGlow = userShadows.map {
                            auraGlowLayer(it, userDotPx, userDotPx, Offset.Zero)
                        }

                        onDrawBehind {
                            livePoints.forEachIndexed { index, point ->
                                drawGlowingDot(
                                    glow = liveGlow,
                                    center = point,
                                    color = colors.mapDotLive,
                                    coreRadius = liveDotPx / 2f,
                                    phase = pulse.value + index * 0.37f,
                                )
                            }

                            if (userPoint != null) {
                                drawGlowingDot(
                                    glow = userGlow,
                                    center = userPoint,
                                    color = colors.mapDotUser,
                                    coreRadius = userDotPx / 2f,
                                    phase = pulse.value,
                                    alpha = presenceAlpha.value,
                                )
                            }
                        }
                    }
            )
        }
    }
}

private fun List<Offset>.toPointArray(size: Size): FloatArray {
    val coordinates = FloatArray(this.size * 2)
    forEachIndexed { index, dot ->
        coordinates[index * 2] = dot.x * size.width
        coordinates[index * 2 + 1] = dot.y * size.height
    }
    return coordinates
}

private fun pointPaint(color: Color, strokeWidth: Float): NativePaint {
    val paint = Paint().asFrameworkPaint()
    paint.isAntiAlias = true
    paint.color = color.toArgb()
    paint.style = AndroidPaint.Style.STROKE
    paint.strokeWidth = strokeWidth
    paint.strokeCap = AndroidPaint.Cap.ROUND
    return paint
}

private fun List<Offset>.scaledTo(size: Size): List<Offset> =
    map { Offset(it.x * size.width, it.y * size.height) }

private fun Offset.scaledTo(size: Size): Offset = Offset(x * size.width, y * size.height)

private fun breathe(phase: Float): Float = (sin(phase * 2f * PI.toFloat()) + 1f) / 2f

private fun DrawScope.drawGlowingDot(
    glow: List<AuraGlowLayer>,
    center: Offset,
    color: Color,
    coreRadius: Float,
    phase: Float,
    alpha: Float = 1f,
) {
    if (alpha <= 0f) return

    val amplitude = (GLOW_FLOOR + GLOW_SWING * breathe(phase)) * alpha
    glow.forEach { drawAuraGlow(it, center, amplitude) }
    drawCircle(color = color.copy(alpha = alpha), radius = coreRadius, center = center)
}
