package com.aura.feature.network.presentation.components

import com.aura.feature.network.domain.model.PingRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val WIDTH = 100f

private const val TOP = 0f

private const val BOTTOM = 200f

class PingChartTest {

    @Test
    fun `an empty journal draws no points`() {
        assertTrue(emptyList<PingRecord>().toPoints(WIDTH, TOP, BOTTOM).isEmpty())
    }

    @Test
    fun `a single record sits in the middle of the plot`() {
        val point = records(35).toPoints(WIDTH, TOP, BOTTOM).single()

        assertEquals(50f, point.x, 0.0001f)
    }

    @Test
    fun `records spread evenly from one edge to the other`() {
        val points = records(20, 20, 20).toPoints(WIDTH, TOP, BOTTOM)

        assertEquals(listOf(0f, 50f, 100f), points.map { it.x })
    }

    @Test
    fun `a shorter journal still spans the whole plot`() {
        val points = records(20, 20).toPoints(WIDTH, TOP, BOTTOM)

        assertEquals(listOf(0f, 100f), points.map { it.x })
    }

    @Test
    fun `a ping of nothing sits on the bottom line`() {
        assertEquals(BOTTOM, records(0).toPoints(WIDTH, TOP, BOTTOM).single().y, 0.0001f)
    }

    @Test
    fun `a ping at the top of the axis sits on the top line`() {
        assertEquals(TOP, records(70).toPoints(WIDTH, TOP, BOTTOM).single().y, 0.0001f)
    }

    @Test
    fun `a ping over the axis is pinned to the top line`() {
        assertEquals(TOP, records(240).toPoints(WIDTH, TOP, BOTTOM).single().y, 0.0001f)
    }

    @Test
    fun `half the axis is half the plot`() {
        assertEquals(100f, records(35).toPoints(WIDTH, TOP, BOTTOM).single().y, 0.0001f)
    }

    @Test
    fun `the plot is measured from the gridlines it is given`() {
        val point = records(70).toPoints(WIDTH, top = 20f, bottom = 120f).single()

        assertEquals(20f, point.y, 0.0001f)
    }

    private fun records(vararg pingMs: Int): List<PingRecord> = pingMs.map {
        PingRecord(
            timestamp = 1_754_000_000_000,
            ipAddress = "192.168.1.42",
            operator = "T-Mobile",
            pingMs = it,
            location = "Miami, US",
            vpnActive = false,
        )
    }
}
