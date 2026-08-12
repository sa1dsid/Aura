package com.aura.feature.network.data.local

import com.aura.feature.network.domain.model.PING_HISTORY_LIMIT
import com.aura.feature.network.domain.model.PingRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class PingHistoryCapTest {

    @Test
    fun `keeps the journal at fifty records and drops the oldest`() {
        var journal = emptyList<PingRecord>()

        repeat(PING_HISTORY_LIMIT + 12) { index -> journal = journal.appendCapped(record(index)) }

        assertEquals(PING_HISTORY_LIMIT, journal.size)
        assertEquals(12, journal.first().pingMs)
        assertEquals(PING_HISTORY_LIMIT + 11, journal.last().pingMs)
    }

    @Test
    fun `appends to the end so the latest record stays last`() {
        val journal = listOf(record(1)).appendCapped(record(2))

        assertEquals(listOf(1, 2), journal.map { it.pingMs })
    }

    private fun record(pingMs: Int): PingRecord = PingRecord(
        timestamp = 1_754_000_000_000 + pingMs,
        ipAddress = "192.168.1.42",
        operator = "T-Mobile",
        pingMs = pingMs,
        location = "London, UK",
        vpnActive = false,
    )
}
