package com.aura.feature.network.presentation.format

import com.aura.feature.network.domain.model.PingRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NetworkLogTest {

    @Test
    fun `opens every day with a comment carrying that day's count`() {
        val records = listOf(
            record(day = 10, hour = 8),
            record(day = 10, hour = 13),
            record(day = 10, hour = 21),
            record(day = 11, hour = 9),
        )

        val lines = records.toLogLines()

        assertEquals(3, (lines[0] as NetworkLogLine.DayComment).count)
        assertEquals(1, (lines[4] as NetworkLogLine.DayComment).count)
        assertEquals(6, lines.size)
    }

    @Test
    fun `keeps records in journal order under their day`() {
        val records = listOf(record(day = 10, hour = 8), record(day = 11, hour = 9))

        val entries = records.toLogLines().filterIsInstance<NetworkLogLine.Entry>()

        assertEquals(records, entries.map { it.record })
    }

    @Test
    fun `shows nothing for an empty journal`() {
        assertEquals(emptyList<NetworkLogLine>(), emptyList<PingRecord>().toLogLines())
    }

    private fun record(day: Int, hour: Int): PingRecord = PingRecord(
        timestamp = millisAt(day, hour),
        ipAddress = "192.168.1.42",
        operator = "T-Mobile",
        pingMs = 27,
        location = "Miami, US",
        vpnActive = false,
    )

    private fun millisAt(day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.JULY, day, hour, 14)
    }.timeInMillis
}
