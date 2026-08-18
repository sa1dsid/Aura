package com.aura.feature.home.presentation.components.mesh

import android.content.Context
import android.graphics.BitmapFactory
import com.aura.R
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

internal object WorldLandmass {

    private val cache = AtomicReference<Pair<CacheKey, MeshDotGrid>?>(null)

    fun cached(projection: WorldProjection, columns: Int): MeshDotGrid? =
        cache.get()?.takeIf { it.first == CacheKey(projection, columns) }?.second

    fun dotGrid(context: Context, projection: WorldProjection, columns: Int): MeshDotGrid {
        cached(projection, columns)?.let { return it }

        val grid = decode(context, projection, columns)
        cache.set(CacheKey(projection, columns) to grid)
        return grid
    }

    private fun decode(context: Context, projection: WorldProjection, columns: Int): MeshDotGrid {
        val rows = (columns / projection.aspectRatio).roundToInt().coerceAtLeast(1)
        val mask = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.world_mask,
            BitmapFactory.Options().apply { inScaled = false },
        ) ?: return MeshDotGrid(columns, rows, BooleanArray(columns * rows))

        val maskWidth = mask.width
        val maskHeight = mask.height
        val pixels = IntArray(maskWidth * maskHeight)
        mask.getPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)
        mask.recycle()

        val land = BooleanArray(columns * rows)
        for (row in 0 until rows) {
            val y = (((row + 0.5f) / rows) * maskHeight).toInt().coerceIn(0, maskHeight - 1)
            for (column in 0 until columns) {
                val x = (((column + 0.5f) / columns) * maskWidth).toInt().coerceIn(0, maskWidth - 1)
                land[row * columns + column] = (pixels[y * maskWidth + x] and 0xFF) > LAND_THRESHOLD
            }
        }

        return MeshDotGrid(columns = columns, rows = rows, land = land)
    }

    private data class CacheKey(val projection: WorldProjection, val columns: Int)

    private const val LAND_THRESHOLD = 127
}
