package com.aura.feature.network.data.local

import com.aura.feature.network.domain.model.PingRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class PingRecordCodecTest {

    @Test
    fun `survives a round trip`() {
        val records = listOf(
            PingRecord(1_754_000_000_000, "192.168.1.42", "T-Mobile", 27, "Miami, US", false),
            PingRecord(1_754_000_600_000, "10.0.0.7", "Vodafone", 84, "London, UK", true),
        )

        assertEquals(records, records.encodeRecords().decodeRecords())
    }

    @Test
    fun `keeps missing fields null instead of empty strings`() {
        val record = PingRecord(1_754_000_000_000, null, null, 31, null, false)

        val restored = listOf(record).encodeRecords().decodeRecords().single()

        assertEquals(record, restored)
    }

    @Test
    fun `reads an empty store as an empty journal`() {
        assertEquals(emptyList<PingRecord>(), null.decodeRecords())
        assertEquals(emptyList<PingRecord>(), "".decodeRecords())
    }

    @Test
    fun `drops damaged lines instead of failing the whole journal`() {
        val healthy = PingRecord(1_754_000_000_000, "10.0.0.7", "Vodafone", 41, "London, UK", false)
        val damaged = listOf(healthy).encodeRecords() + "what is this"

        assertEquals(listOf(healthy), damaged.decodeRecords())
    }
}
