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

    @Test
    fun `opens a single record with one comment`() {
        val lines = listOf(record(day = 10, hour = 8)).toLogLines()

        assertEquals(2, lines.size)
        assertEquals(1, (lines[0] as NetworkLogLine.DayComment).count)
    }

    @Test
    fun `gives every day its own comment`() {
        val records = listOf(
            record(day = 10, hour = 8),
            record(day = 11, hour = 9),
            record(day = 12, hour = 10),
        )

        val comments = records.toLogLines().filterIsInstance<NetworkLogLine.DayComment>()

        assertEquals(3, comments.size)
        assertEquals(listOf(1, 1, 1), comments.map { it.count })
    }

    @Test
    fun `a comment carries the time of the first record of its day`() {
        val first = record(day = 10, hour = 8)
        val records = listOf(first, record(day = 10, hour = 21))

        val comment = records.toLogLines().first() as NetworkLogLine.DayComment

        assertEquals(first.timestamp, comment.timestamp)
    }

    @Test
    fun `a day that comes back later opens a comment of its own`() {
        val records = listOf(
            record(day = 10, hour = 8),
            record(day = 11, hour = 9),
            record(day = 10, hour = 10),
        )

        val comments = records.toLogLines().filterIsInstance<NetworkLogLine.DayComment>()

        assertEquals(3, comments.size)
        assertEquals(listOf(1, 1, 1), comments.map { it.count })
    }

    @Test
    fun `every record of the journal keeps a line of its own`() {
        val records = List(7) { record(day = 10 + it / 3, hour = 8 + it) }

        val entries = records.toLogLines().filterIsInstance<NetworkLogLine.Entry>()

        assertEquals(7, entries.size)
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
