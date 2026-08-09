package com.aura.core.designsystem.component

import android.graphics.BlurMaskFilter
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraColors
import kotlin.math.exp

private const val SIGMA_TO_MASK_RADIUS = 0.57735f

private const val MASK_RADIUS_BIAS = 0.5f

private const val FALLBACK_RING_COUNT = 10

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
): Modifier = drawWithCache {
    val glowCenter = Offset(size.width / 2f, size.height / 2f + offsetY.toPx())
    val layer = glowLayer(color, blurRadius.toPx(), width.toPx(), height.toPx(), glowCenter)

    onDrawBehind { drawGlowLayer(layer) }
}

fun Modifier.auraGlowLayers(
    shadows: List<AuraShadow>,
    coreSize: Dp? = null,
): Modifier = drawWithCache {
    val glowCenter = Offset(size.width / 2f, size.height / 2f)
    val baseWidth = coreSize?.toPx() ?: size.width
    val baseHeight = coreSize?.toPx() ?: size.height
    val layers = shadows.map { shadow ->
        val spread = shadow.spread.toPx()
        glowLayer(
            color = shadow.color,
            blur = shadow.blurRadius.toPx(),
            width = baseWidth + spread * 2f,
            height = baseHeight + spread * 2f,
            center = glowCenter,
        )
    }

    onDrawBehind { layers.forEach { drawGlowLayer(it) } }
}

fun Modifier.auraDropShadow(
    color: Color,
    blurRadius: Dp,
    cornerRadius: Dp,
    spread: Dp = 0.dp,
): Modifier = drawWithCache {
    val paint = if (HARDWARE_BLUR_SUPPORTED) blurPaint(color, blurRadius.toPx()) else null
    val blur = blurRadius.toPx()
    val grow = spread.toPx()
    val corner = cornerRadius.toPx() + grow

    onDrawBehind {
        if (paint != null) {
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
                alpha = color.alpha * fade * 0.09f,
            )
        }
    }
}

private fun DrawScope.drawGlowLayer(layer: GlowLayer) {
    if (layer.width <= 0f || layer.height <= 0f) return

    val paint = layer.paint
    if (paint != null) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawOval(
                layer.center.x - layer.width / 2f,
                layer.center.y - layer.height / 2f,
                layer.center.x + layer.width / 2f,
                layer.center.y + layer.height / 2f,
                paint,
            )
        }
        return
    }

    val brush = layer.brush ?: return
    val outerWidth = layer.width + layer.blur * 2f
    val outerHeight = layer.height + layer.blur * 2f
    withTransform({
        scale(scaleX = 1f, scaleY = outerHeight / outerWidth, pivot = layer.center)
    }) {
        drawCircle(brush = brush, radius = outerWidth / 2f, center = layer.center)
    }
}

private class GlowLayer(
    val paint: NativePaint?,
    val brush: Brush?,
    val width: Float,
    val height: Float,
    val blur: Float,
    val center: Offset,
)

private fun glowLayer(
    color: Color,
    blur: Float,
    width: Float,
    height: Float,
    center: Offset,
) = GlowLayer(
    paint = if (HARDWARE_BLUR_SUPPORTED) blurPaint(color, blur) else null,
    brush = if (HARDWARE_BLUR_SUPPORTED) null else fadeBrush(color, blur, width, height, center),
    width = width,
    height = height,
    blur = blur,
    center = center,
)

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
    height: Float,
    center: Offset,
): Brush {
    val outerRadius = (width + blur * 2f) / 2f
    val coreFraction = (width / 2f) / outerRadius
    val sigma = (blur / 2f).coerceAtLeast(0.01f)
    val coreRadius = minOf(width, height) / 2f
    val peak = color.alpha * (1f - exp(-(coreRadius * coreRadius) / (2f * sigma * sigma)))

    return Brush.radialGradient(
        colorStops = arrayOf(
            0f to color.copy(alpha = peak),
            coreFraction * 0.55f to color.copy(alpha = peak * 0.9f),
            coreFraction to color.copy(alpha = peak * 0.42f),
            (coreFraction + 1f) / 2f to color.copy(alpha = peak * 0.14f),
            1f to Color.Transparent,
        ),
        center = center,
        radius = outerRadius,
    )
}
