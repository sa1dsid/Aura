package com.aura.core.designsystem.component

import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraColors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val SIGMA_TO_MASK_RADIUS = 0.57735f

private const val MASK_RADIUS_BIAS = 0.5f

private const val FALLBACK_RING_COUNT = 10

private const val FALLBACK_STROKE_COUNT = 16

private const val FALLBACK_STROKE_REACH = 3f

private const val FALLBACK_STOP_COUNT = 16

private const val SIGMA_TO_PEAK = 2.5066f

private const val MIN_SIGMA = 0.01f

private const val GLOW_EXTENT_SIGMAS = 3f

private const val ERF_APPROXIMATION = 0.147f

private val SQRT_TWO = sqrt(2f)

private val HARDWARE_BLUR_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

data class AuraShadow(
    val color: Color,
    val blurRadius: Dp,
    val spread: Dp = 0.dp,
)

val AuraColors.signalDotShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(accentBlue.copy(alpha = 0.20f), 3.dp),
        AuraShadow(glowCyan.copy(alpha = 0.10f), 6.dp, 1.dp),
        AuraShadow(Color.White.copy(alpha = 0.30f), 20.dp, 3.dp),
    )

val AuraColors.brightDotShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(glowIce, 3.dp),
        AuraShadow(glowIce.copy(alpha = 0.80f), 6.dp, 1.dp),
        AuraShadow(glowIce.copy(alpha = 0.40f), 20.dp, 3.dp),
    )

val AuraColors.activeDotShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(green, 1.dp),
        AuraShadow(green, 4.dp),
        AuraShadow(Color.White.copy(alpha = 0.20f), 8.dp, 1.dp),
    )

val AuraColors.balanceDotShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(accentBlue, 1.dp),
        AuraShadow(glowCyan.copy(alpha = 0.80f), 4.dp),
        AuraShadow(Color.White.copy(alpha = 0.20f), 8.dp, 1.dp),
    )

val AuraColors.avatarShadows: List<AuraShadow>
    get() = listOf(AuraShadow(Color.White.copy(alpha = 0.40f), 10.dp))

val AuraColors.leadAvatarShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(glowFrost.copy(alpha = 0.70f), 2.dp),
        AuraShadow(Color.White.copy(alpha = 0.40f), 10.dp),
    )

val AuraColors.planetRingShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(glowSky, 1.dp),
        AuraShadow(glowSky, 10.dp, 1.dp),
        AuraShadow(glowSky, 31.dp, 3.dp),
    )

val AuraColors.planetCoreShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(iceBlue.copy(alpha = 0.20f), 120.dp),
        AuraShadow(glowSky.copy(alpha = 0.04f), 10.dp),
        AuraShadow(glowSky.copy(alpha = 0.12f), 20.dp, 2.dp),
    )

fun Modifier.auraGlow(
    color: Color,
    width: Dp,
    height: Dp,
    blurRadius: Dp = 64.dp,
    offsetY: Dp = 0.dp,
    alpha: () -> Float = { 1f },
): Modifier = drawWithCache {
    val glowCenter = Offset(size.width / 2f, size.height / 2f + offsetY.toPx())
    val layer = glowLayer(color, blurRadius.toPx(), width.toPx(), height.toPx(), glowCenter)

    onDrawBehind { drawAuraGlow(layer, alpha = alpha()) }
}

fun Modifier.auraGlowLayers(
    shadows: List<AuraShadow>,
    coreSize: Dp? = null,
): Modifier = drawWithCache {
    val glowCenter = Offset(size.width / 2f, size.height / 2f)
    val baseWidth = coreSize?.toPx() ?: size.width
    val baseHeight = coreSize?.toPx() ?: size.height
    val layers = shadows.map { auraGlowLayer(it, baseWidth, baseHeight, glowCenter) }

    onDrawBehind { layers.forEach { drawAuraGlow(it) } }
}

fun Density.auraGlowLayer(
    shadow: AuraShadow,
    width: Float,
    height: Float,
    center: Offset,
): AuraGlowLayer {
    val spread = shadow.spread.toPx()
    return glowLayer(
        color = shadow.color,
        blur = shadow.blurRadius.toPx(),
        width = width + spread * 2f,
        height = height + spread * 2f,
        center = center,
    )
}

fun Density.auraBlurRadius(blurRadius: Dp): Dp {
    val sigma = blurRadius.toPx() / 2f
    return ((sigma - MASK_RADIUS_BIAS) / SIGMA_TO_MASK_RADIUS).coerceAtLeast(0f).toDp()
}

fun Density.auraArcGlow(shadow: AuraShadow, strokeWidth: Float): AuraArcGlow {
    val blur = shadow.blurRadius.toPx()
    val width = strokeWidth + shadow.spread.toPx() * 2f

    return AuraArcGlow(
        paint = if (HARDWARE_BLUR_SUPPORTED) arcPaint(shadow.color, blur, width) else null,
        color = shadow.color,
        passes = if (HARDWARE_BLUR_SUPPORTED) emptyList() else arcGlowPasses(width, blur),
    )
}

fun Modifier.auraDropShadow(
    color: Color,
    blurRadius: Dp,
    cornerRadius: Dp,
    spread: Dp = 0.dp,
    alpha: () -> Float = { 1f },
): Modifier = drawWithCache {
    val paint = if (HARDWARE_BLUR_SUPPORTED) blurPaint(color, blurRadius.toPx()) else null
    val blur = blurRadius.toPx()
    val grow = spread.toPx()
    val corner = cornerRadius.toPx() + grow

    onDrawBehind {
        val fraction = alpha()
        if (fraction <= 0f) return@onDrawBehind

        if (paint != null) {
            paint.alpha = (color.alpha * fraction * 255f).roundToInt().coerceIn(0, 255)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRoundRect(
                    -grow, -grow, size.width + grow, size.height + grow, corner, corner, paint,
                )
            }
            return@onDrawBehind
        }

        for (ring in FALLBACK_RING_COUNT downTo 1) {
            val offset = grow + blur * ring / FALLBACK_RING_COUNT
            val fade = 1f - ring.toFloat() / FALLBACK_RING_COUNT
            drawRoundRect(
                color = color,
                topLeft = Offset(-offset, -offset),
                size = Size(size.width + offset * 2, size.height + offset * 2),
                cornerRadius = CornerRadius(cornerRadius.toPx() + offset),
                alpha = color.alpha * fade * 0.09f * fraction,
            )
        }
    }
}

fun Modifier.auraDropShadows(
    shadows: List<AuraShadow>,
    cornerRadius: Dp,
    alpha: () -> Float = { 1f },
): Modifier = shadows.fold(this) { chain, shadow ->
    chain.auraDropShadow(
        color = shadow.color,
        blurRadius = shadow.blurRadius,
        cornerRadius = cornerRadius,
        spread = shadow.spread,
        alpha = alpha,
    )
}

fun DrawScope.drawAuraArcGlow(
    glow: AuraArcGlow,
    topLeft: Offset,
    size: Size,
    startAngle: Float,
    sweepAngle: Float,
    alpha: Float = 1f,
) {
    if (sweepAngle == 0f) return

    val paint = glow.paint
    if (paint != null) {
        paint.alpha = (glow.color.alpha * alpha * 255f).roundToInt().coerceIn(0, 255)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawArc(
                topLeft.x,
                topLeft.y,
                topLeft.x + size.width,
                topLeft.y + size.height,
                startAngle,
                sweepAngle,
                false,
                paint,
            )
        }
        return
    }

    glow.passes.forEach { pass ->
        drawArc(
            color = glow.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = pass.strokeWidth, cap = StrokeCap.Round),
            alpha = pass.coverage * alpha,
        )
    }
}

fun DrawScope.drawAuraGlow(
    layer: AuraGlowLayer,
    center: Offset = layer.center,
    alpha: Float = 1f,
) {
    if (layer.width <= 0f || layer.height <= 0f || alpha <= 0f) return

    val paint = layer.paint
    if (paint != null) {
        paint.alpha = (layer.alpha * alpha * 255f).roundToInt().coerceIn(0, 255)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawOval(
                center.x - layer.width / 2f,
                center.y - layer.height / 2f,
                center.x + layer.width / 2f,
                center.y + layer.height / 2f,
                paint,
            )
        }
        return
    }

    val brush = layer.brush ?: return
    val outerWidth = layer.width + GLOW_EXTENT_SIGMAS * layer.blur
    val outerHeight = layer.height + GLOW_EXTENT_SIGMAS * layer.blur
    withTransform({
        translate(center.x - layer.center.x, center.y - layer.center.y)
        scale(scaleX = 1f, scaleY = outerHeight / outerWidth, pivot = layer.center)
    }) {
        drawCircle(
            brush = brush,
            radius = outerWidth / 2f,
            center = layer.center,
            alpha = alpha,
        )
    }
}

class AuraGlowLayer internal constructor(
    internal val paint: NativePaint?,
    internal val brush: Brush?,
    internal val width: Float,
    internal val height: Float,
    internal val blur: Float,
    internal val center: Offset,
    internal val alpha: Float,
)

class AuraArcGlow internal constructor(
    internal val paint: NativePaint?,
    internal val color: Color,
    internal val passes: List<ArcGlowPass>,
)

internal class ArcGlowPass(val strokeWidth: Float, val coverage: Float)

private fun glowLayer(
    color: Color,
    blur: Float,
    width: Float,
    height: Float,
    center: Offset,
) = AuraGlowLayer(
    paint = if (HARDWARE_BLUR_SUPPORTED) blurPaint(color, blur) else null,
    brush = if (HARDWARE_BLUR_SUPPORTED) null else fadeBrush(color, blur, width, center),
    width = width,
    height = height,
    blur = blur,
    center = center,
    alpha = color.alpha,
)

private fun arcPaint(color: Color, blur: Float, strokeWidth: Float): NativePaint {
    val paint = blurPaint(color, blur)
    paint.style = AndroidPaint.Style.STROKE
    paint.strokeWidth = strokeWidth
    paint.strokeCap = AndroidPaint.Cap.ROUND
    return paint
}

private fun arcGlowPasses(strokeWidth: Float, blur: Float): List<ArcGlowPass> {
    val sigma = (blur / 2f).coerceAtLeast(MIN_SIGMA)
    val half = strokeWidth / 2f
    val step = sigma * FALLBACK_STROKE_REACH / FALLBACK_STROKE_COUNT
    val peak = (strokeWidth / (sigma * SIGMA_TO_PEAK)).coerceAtMost(1f)

    fun coverage(distance: Float): Float {
        val outside = (distance - half).coerceAtLeast(0f)
        return peak * exp(-(outside * outside) / (2f * sigma * sigma))
    }

    return (FALLBACK_STROKE_COUNT downTo 1).map { index ->
        ArcGlowPass(
            strokeWidth = (half + index * step) * 2f,
            coverage = (coverage(half + (index - 0.5f) * step) - coverage(half + (index + 0.5f) * step))
                .coerceIn(0f, 1f),
        )
    }
}

private fun blurPaint(color: Color, blur: Float): NativePaint {
    val paint = Paint().asFrameworkPaint()
    paint.isAntiAlias = true
    paint.color = color.toArgb()

    val maskRadius = (blur / 2f - MASK_RADIUS_BIAS) / SIGMA_TO_MASK_RADIUS
    if (maskRadius > 0f) {
        paint.maskFilter = BlurMaskFilter(maskRadius, BlurMaskFilter.Blur.NORMAL)
    }

    return paint
}

private fun fadeBrush(
    color: Color,
    blur: Float,
    width: Float,
    center: Offset,
): Brush {
    val coreRadius = width / 2f
    val sigma = (blur / 2f).coerceAtLeast(MIN_SIGMA)
    val outerRadius = coreRadius + GLOW_EXTENT_SIGMAS * sigma
    val peak = color.alpha * (1f - exp(-(coreRadius * coreRadius) / (2f * sigma * sigma)))
    val edge = normalCdf(coreRadius / sigma)

    val stops = Array(FALLBACK_STOP_COUNT + 1) { index ->
        val fraction = index.toFloat() / FALLBACK_STOP_COUNT
        val falloff = if (edge > 0f) {
            normalCdf((coreRadius - fraction * outerRadius) / sigma) / edge
        } else {
            0f
        }
        fraction to color.copy(alpha = peak * falloff)
    }

    return Brush.radialGradient(
        colorStops = stops,
        center = center,
        radius = outerRadius,
    )
}

private fun normalCdf(x: Float): Float = 0.5f * (1f + erf(x / SQRT_TWO))

private fun erf(x: Float): Float {
    val square = x * x
    val shaped = square * (4f / PI.toFloat() + ERF_APPROXIMATION * square) /
        (1f + ERF_APPROXIMATION * square)
    val magnitude = sqrt(1f - exp(-shaped))
    return if (x < 0f) -magnitude else magnitude
}
