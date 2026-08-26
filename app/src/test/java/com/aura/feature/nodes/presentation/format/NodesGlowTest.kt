package com.aura.feature.nodes.presentation.format

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class NodesGlowTest {

    @Test
    fun `a figma blur loses a fifth on the way into the app`() {
        assertEquals(0.8f, 1.dp.figmaBlur().value, TOLERANCE)
        assertEquals(3.2f, 4.dp.figmaBlur().value, TOLERANCE)
        assertEquals(16f, 20.dp.figmaBlur().value, TOLERANCE)
    }

    @Test
    fun `a zero blur stays zero`() {
        assertEquals(0f, 0.dp.figmaBlur().value, TOLERANCE)
    }

    @Test
    fun `the synthetic italic keeps the skew the design was drawn with`() {
        assertEquals(NODES_ITALIC_SKEW, NodesItalic.skewX, TOLERANCE)
        assertEquals(1f, NodesItalic.scaleX, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
