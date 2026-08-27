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

    @Test
    fun `writes an empty journal as an empty store`() {
        assertEquals("", emptyList<PingRecord>().encodeRecords())
    }

    @Test
    fun `keeps a comma inside a city name`() {
        val record = PingRecord(1_754_000_000_000, "10.0.0.7", "Vodafone", 41, "Miami, US", false)

        assertEquals(record, listOf(record).encodeRecords().decodeRecords().single())
    }

    @Test
    fun `drops a record whose city carries the field separator`() {
        val poisoned =
            PingRecord(1_754_000_000_000, "10.0.0.7", "Vodafone", 41, "MiamiUS", false)
        val healthy = PingRecord(1_754_000_060_000, "10.0.0.8", "Vodafone", 42, "London, UK", false)

        val restored = listOf(poisoned, healthy).encodeRecords().decodeRecords()

        assertEquals(listOf(healthy), restored)
    }

    @Test
    fun `drops a record whose city carries the record separator`() {
        val poisoned =
            PingRecord(1_754_000_000_000, "10.0.0.7", "Vodafone", 41, "MiamiUS", false)

        assertEquals(emptyList<PingRecord>(), listOf(poisoned).encodeRecords().decodeRecords())
    }

    @Test
    fun `drops a record whose time cannot be read`() {
        val healthy = PingRecord(1_754_000_000_000, "10.0.0.7", "Vodafone", 41, "London, UK", false)
        val damaged = "later10.0.0.7Vodafone41London, UK"

        assertEquals(
            listOf(healthy),
            (listOf(healthy).encodeRecords() + "" + damaged).decodeRecords(),
        )
    }

    @Test
    fun `keeps the vpn flag of every record apart`() {
        val records = listOf(
            PingRecord(1_754_000_000_000, "10.0.0.7", "Vodafone", 41, "London, UK", true),
            PingRecord(1_754_000_060_000, "10.0.0.7", "Vodafone", 42, "London, UK", false),
        )

        assertEquals(
            listOf(true, false),
            records.encodeRecords().decodeRecords().map { it.vpnActive },
        )
    }
}
