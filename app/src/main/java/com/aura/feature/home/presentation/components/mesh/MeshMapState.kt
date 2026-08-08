package com.aura.feature.home.presentation.components.mesh

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.util.lerp

@Stable
class MeshMapState internal constructor(
    private val minScale: Float,
    private val maxScale: Float,
) {

    var scale by mutableFloatStateOf(minScale)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    private var viewportSize by mutableStateOf(Size.Zero)

    val isZoomed: Boolean get() = scale > minScale * 1.01f

    fun onViewportChanged(size: Size) {
        viewportSize = size
        offset = offset.clampedTo(scale)
    }

    fun onGesture(centroid: Offset, pan: Offset, zoomChange: Float) {
        val nextScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        val scaleRatio = if (scale == 0f) 1f else nextScale / scale
        val fromCenter = centroid - Offset(viewportSize.width / 2f, viewportSize.height / 2f)

        scale = nextScale
        offset = (fromCenter + pan - (fromCenter - offset) * scaleRatio).clampedTo(nextScale)
    }

    suspend fun animateToWorldView() {
        val startScale = scale
        val startOffset = offset
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(RESET_DURATION_MILLIS, easing = FastOutSlowInEasing),
        ) { fraction, _ ->
            scale = lerp(startScale, minScale, fraction)
            offset = lerp(startOffset, Offset.Zero, fraction)
        }
    }

    private fun Offset.clampedTo(forScale: Float): Offset {
        val maxX = (viewportSize.width * (forScale - 1f)) / 2f
        val maxY = (viewportSize.height * (forScale - 1f)) / 2f
        if (maxX <= 0f || maxY <= 0f) return Offset.Zero
        return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
    }

    private companion object {
        const val RESET_DURATION_MILLIS = 320
    }
}

@Composable
fun rememberMeshMapState(
    minScale: Float = MeshMapDefaults.MIN_SCALE,
    maxScale: Float = MeshMapDefaults.MAX_SCALE,
): MeshMapState = remember(minScale, maxScale) { MeshMapState(minScale, maxScale) }
