package com.aura.feature.transactions.presentation.format

import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransactionLogTest {

    @Test
    fun `an empty log has no lines at all`() {
        assertTrue(emptyList<TransactionEvent>().toLogLines().isEmpty())
    }

    @Test
    fun `a single event gets its own day comment`() {
        val lines = listOf(event(id = "1", day = 10)).toLogLines()

        assertEquals(2, lines.size)
        assertEquals(1, (lines.first() as TransactionLogLine.DayComment).count)
        assertEquals("1", (lines.last() as TransactionLogLine.Entry).event.id)
    }

    @Test
    fun `each day opens with a comment counting its events`() {
        val lines = listOf(
            event(id = "1", day = 10, hour = 14),
            event(id = "2", day = 10, hour = 12),
            event(id = "3", day = 10, hour = 8),
            event(id = "4", day = 9, hour = 22),
        ).toLogLines()

        assertEquals(listOf(3, 1), lines.comments().map { it.count })
        assertEquals(listOf("1", "2", "3", "4"), lines.entries().map { it.event.id })
        assertEquals(6, lines.size)
    }

    @Test
    fun `the comment carries the timestamp of the first event of its day`() {
        val first = event(id = "1", day = 10, hour = 14)
        val lines = listOf(first, event(id = "2", day = 10, hour = 12)).toLogLines()

        assertEquals(first.timestamp, lines.comments().single().timestamp)
    }

    @Test
    fun `a day that comes back later opens a second comment`() {
        val lines = listOf(
            event(id = "1", day = 10),
            event(id = "2", day = 9),
            event(id = "3", day = 10),
        ).toLogLines()

        assertEquals(listOf(1, 1, 1), lines.comments().map { it.count })
        assertEquals(listOf("1", "2", "3"), lines.entries().map { it.event.id })
    }

    @Test
    fun `events keep the order they were given in`() {
        val lines = listOf(
            event(id = "3", day = 8),
            event(id = "1", day = 10),
            event(id = "2", day = 9),
        ).toLogLines()

        assertEquals(listOf("3", "1", "2"), lines.entries().map { it.event.id })
    }

    private fun List<TransactionLogLine>.comments() =
        filterIsInstance<TransactionLogLine.DayComment>()

    private fun List<TransactionLogLine>.entries() =
        filterIsInstance<TransactionLogLine.Entry>()

    private fun event(id: String, day: Int, hour: Int = 12) = TransactionEvent(
        id = id,
        timestamp = localMillis(day = day, hour = hour),
        kind = TransactionKind.ION,
        detail = "tap_reward",
        amount = 20,
        currency = "ION",
        isCredit = true,
    )

    private fun localMillis(day: Int, hour: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(2026, Calendar.AUGUST, day, hour, 0, 0)
        return calendar.timeInMillis
    }
}
