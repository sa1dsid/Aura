package com.aura.feature.home.presentation.components.mesh

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

@Immutable
class MeshDotGrid(
    val columns: Int,
    val rows: Int,
    private val land: BooleanArray,
) {

    val dots: List<Offset> = buildList(land.count { it }) {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                if (land[row * columns + column]) add(centerOf(column, row))
            }
        }
    }

    fun snap(position: Offset): Offset {
        var nearest = position
        var nearestDistance = Float.MAX_VALUE
        dots.forEach { dot ->
            val distance = squaredCellDistance(dot, position)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearest = dot
            }
        }
        return nearest
    }

    private fun centerOf(column: Int, row: Int): Offset = Offset(
        x = (column + 0.5f) / columns,
        y = (row + 0.5f) / rows,
    )

    private fun squaredCellDistance(from: Offset, to: Offset): Float {
        val dx = (from.x - to.x) * columns
        val dy = (from.y - to.y) * rows
        return dx * dx + dy * dy
    }
}
