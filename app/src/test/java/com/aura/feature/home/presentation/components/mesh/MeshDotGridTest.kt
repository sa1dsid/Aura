package com.aura.feature.home.presentation.components.mesh

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class MeshDotGridTest {

    @Test
    fun `dots sit in the middle of their cells`() {
        val grid = gridOf(columns = 4, rows = 2, landCells = listOf(0 to 0, 3 to 1))

        assertEquals(
            listOf(Offset(0.125f, 0.25f), Offset(0.875f, 0.75f)),
            grid.dots,
        )
    }

    @Test
    fun `snap moves the position onto the nearest lit cell`() {
        val grid = gridOf(columns = 4, rows = 2, landCells = listOf(0 to 0, 3 to 0))

        assertEquals(Offset(0.875f, 0.25f), grid.snap(Offset(0.9f, 0.3f)))
        assertEquals(Offset(0.125f, 0.25f), grid.snap(Offset(0.2f, 0.1f)))
    }

    @Test
    fun `snap measures distance in cells, not in normalized units`() {
        val grid = gridOf(columns = 20, rows = 2, landCells = listOf(6 to 0, 0 to 1))

        val snapped = grid.snap(Offset(0.025f, 0.35f))

        assertEquals(
            "Ячейки квадратные на экране, поэтому ближе тот узел, что ближе по сетке",
            Offset(0.025f, 0.75f),
            snapped,
        )
    }

    @Test
    fun `snap keeps the position when there is nothing to snap to`() {
        val grid = gridOf(columns = 4, rows = 2, landCells = emptyList())
        val position = Offset(0.4f, 0.6f)

        assertEquals(position, grid.snap(position))
    }

    private fun gridOf(columns: Int, rows: Int, landCells: List<Pair<Int, Int>>): MeshDotGrid {
        val land = BooleanArray(columns * rows)
        landCells.forEach { (column, row) -> land[row * columns + column] = true }
        return MeshDotGrid(columns = columns, rows = rows, land = land)
    }
}
