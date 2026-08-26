package com.aura.feature.network.presentation.format

import com.aura.feature.network.domain.model.ConnectionGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NetworkFormattersTest {

    @Test
    fun `the clock is shown as hours and minutes`() {
        assertEquals("08:14", millisAt(day = 10, hour = 8, minute = 14).formatClockTime())
    }

    @Test
    fun `the clock never falls back to a twelve hour dial`() {
        assertEquals("21:47", millisAt(day = 10, hour = 21, minute = 47).formatClockTime())
    }

    @Test
    fun `midnight is shown with two zeroes`() {
        assertEquals("00:05", millisAt(day = 10, hour = 0, minute = 5).formatClockTime())
    }

    @Test
    fun `a log day is a short month and a number`() {
        assertEquals("Jul 10", millisAt(day = 10, hour = 8).formatLogDay())
    }

    @Test
    fun `a badge day shouts the month`() {
        assertEquals("JUL 10", millisAt(day = 10, hour = 8).formatBadgeDay())
    }

    @Test
    fun `two moments of one day are the same day`() {
        assertTrue(
            millisAt(day = 10, hour = 0, minute = 1)
                .isSameDayAs(millisAt(day = 10, hour = 23, minute = 59))
        )
    }

    @Test
    fun `midnight splits two days apart`() {
        assertFalse(
            millisAt(day = 10, hour = 23, minute = 59)
                .isSameDayAs(millisAt(day = 11, hour = 0, minute = 1))
        )
    }

    @Test
    fun `the same day of two different years is not the same day`() {
        assertFalse(
            millisAt(year = 2025, day = 10, hour = 8)
                .isSameDayAs(millisAt(year = 2026, day = 10, hour = 8))
        )
    }

    @Test
    fun `two moments of one day share a day key`() {
        assertEquals(
            millisAt(day = 10, hour = 1).dayKey(),
            millisAt(day = 10, hour = 22).dayKey(),
        )
    }

    @Test
    fun `neighbouring days do not share a day key`() {
        assertNotEquals(
            millisAt(day = 10, hour = 1).dayKey(),
            millisAt(day = 11, hour = 1).dayKey(),
        )
    }

    @Test
    fun `a speed keeps exactly one decimal`() {
        assertEquals("48.6", 48.63.formatSpeed())
        assertEquals("48.0", 48.0.formatSpeed())
        assertEquals("0.0", 0.0.formatSpeed())
    }

    @Test
    fun `a speed rounds to the nearest tenth`() {
        assertEquals("48.7", 48.66.formatSpeed())
    }

    @Test
    fun `a big speed is not shortened`() {
        assertEquals("943.2", 943.21.formatSpeed())
    }

    @Test
    fun `a packet loss keeps exactly one decimal`() {
        assertEquals("33.3", 33.333.formatPacketLoss())
        assertEquals("0.0", 0.0.formatPacketLoss())
    }

    @Test
    fun `a grade is shown with one decimal`() {
        assertEquals("5.0", ConnectionGrade.EXCELLENT.formatScore())
        assertEquals("3.0", ConnectionGrade.GOOD.formatScore())
        assertEquals("1.0", ConnectionGrade.POOR.formatScore())
    }

    @Test
    fun `a grade fills as many dots as its score`() {
        assertEquals(5, ConnectionGrade.EXCELLENT.filledDots)
        assertEquals(3, ConnectionGrade.GOOD.filledDots)
        assertEquals(1, ConnectionGrade.POOR.filledDots)
    }

    private fun millisAt(
        year: Int = 2026,
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Long = Calendar.getInstance().apply {
        clear()
        set(year, Calendar.JULY, day, hour, minute)
    }.timeInMillis
}
