package com.aura.feature.home.presentation.components.mesh

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshMapStateTest {

    private fun state() = MeshMapState(minScale = 1f, maxScale = 6f).apply {
        onViewportChanged(Size(VIEWPORT_WIDTH, VIEWPORT_HEIGHT))
    }

    @Test
    fun `starts at full world view`() {
        val state = state()

        assertEquals(1f, state.scale, TOLERANCE)
        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `zoom is clamped to the allowed range`() {
        val state = state()

        state.onGesture(centroid = center(), pan = Offset.Zero, zoomChange = 100f)
        assertEquals(6f, state.scale, TOLERANCE)

        state.onGesture(centroid = center(), pan = Offset.Zero, zoomChange = 0.001f)
        assertEquals(1f, state.scale, TOLERANCE)
    }

    @Test
    fun `map cannot be dragged while fully zoomed out`() {
        val state = state()

        state.onGesture(centroid = center(), pan = Offset(120f, 80f), zoomChange = 1f)

        assertEquals(Offset.Zero, state.offset)
    }

    @Test
    fun `map cannot be dragged past its own edge`() {
        val state = state()
        state.onGesture(centroid = center(), pan = Offset.Zero, zoomChange = 2f)

        state.onGesture(centroid = center(), pan = Offset(10_000f, 10_000f), zoomChange = 1f)

        assertEquals(VIEWPORT_WIDTH / 2f, state.offset.x, TOLERANCE)
        assertEquals(VIEWPORT_HEIGHT / 2f, state.offset.y, TOLERANCE)
    }

    @Test
    fun `point under the fingers stays under the fingers while zooming`() {
        val state = state()
        val centroid = Offset(VIEWPORT_WIDTH * 0.25f, VIEWPORT_HEIGHT * 0.25f)

        val contentBefore = state.contentPointAt(centroid)
        state.onGesture(centroid = centroid, pan = Offset.Zero, zoomChange = 2f)
        val contentAfter = state.contentPointAt(centroid)

        assertEquals(contentBefore.x, contentAfter.x, 0.5f)
        assertEquals(contentBefore.y, contentAfter.y, 0.5f)
    }

    @Test
    fun `zoomed in flag follows the scale`() {
        val state = state()
        assertTrue(!state.isZoomed)

        state.onGesture(centroid = center(), pan = Offset.Zero, zoomChange = 2f)

        assertTrue(state.isZoomed)
    }

    private fun MeshMapState.contentPointAt(screen: Offset): Offset {
        val viewportCenter = center()
        return viewportCenter + (screen - viewportCenter - offset) / scale
    }

    private fun center() = Offset(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f)

    private companion object {
        const val VIEWPORT_WIDTH = 900f
        const val VIEWPORT_HEIGHT = 420f
        const val TOLERANCE = 0.001f
    }
}
