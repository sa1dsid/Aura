package com.aura.feature.network.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aura.feature.network.domain.model.PING_HISTORY_LIMIT
import com.aura.feature.network.domain.model.PingRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkLocalStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun store(): NetworkLocalStore = NetworkLocalStore(context)

    @Before
    fun emptyTheStore() = runTest { store().clearSession() }

    @Test
    fun `a store nobody wrote to has an empty journal and no quality`() = runTest {
        val store = store()

        assertTrue(store.history.first().isEmpty())
        assertNull(store.quality.first())
    }

    @Test
    fun `an appended record outlives the process`() = runTest {
        store().append(record(pingMs = 27))

        assertEquals(listOf(27), store().history.first().map { it.pingMs })
    }

    @Test
    fun `appending keeps the newest record last`() = runTest {
        val store = store()

        store.append(record(pingMs = 1))
        store.append(record(pingMs = 2))

        assertEquals(listOf(1, 2), store.history.first().map { it.pingMs })
    }

    @Test
    fun `appending never grows the journal past fifty`() = runTest {
        val store = store()

        repeat(PING_HISTORY_LIMIT + 5) { index -> store.append(record(pingMs = index)) }

        val history = store.history.first()

        assertEquals(PING_HISTORY_LIMIT, history.size)
        assertEquals(5, history.first().pingMs)
        assertEquals(PING_HISTORY_LIMIT + 4, history.last().pingMs)
    }

    @Test
    fun `replacing wipes whatever was there`() = runTest {
        val store = store()
        store.append(record(pingMs = 27))

        store.replaceAll(listOf(record(pingMs = 41), record(pingMs = 42)))

        assertEquals(listOf(41, 42), store.history.first().map { it.pingMs })
    }

    @Test
    fun `replacing with nothing empties the journal`() = runTest {
        val store = store()
        store.append(record(pingMs = 27))

        store.replaceAll(emptyList())

        assertTrue(store.history.first().isEmpty())
    }

    @Test
    fun `the measured quality outlives the process`() = runTest {
        store().saveQuality(MeasuredQuality(jitterMs = 9, packetLossPercent = 0.7))

        val quality = store().quality.first()!!

        assertEquals(9, quality.jitterMs)
        assertEquals(0.7, quality.packetLossPercent, 0.0001)
    }

    @Test
    fun `a later quality replaces the one before it`() = runTest {
        val store = store()
        store.saveQuality(MeasuredQuality(jitterMs = 9, packetLossPercent = 0.7))

        store.saveQuality(MeasuredQuality(jitterMs = 3, packetLossPercent = 0.1))

        assertEquals(3, store.quality.first()!!.jitterMs)
    }

    @Test
    fun `closing the session empties the journal and the quality`() = runTest {
        val store = store()
        store.append(record(pingMs = 27))
        store.saveQuality(MeasuredQuality(jitterMs = 9, packetLossPercent = 0.7))

        store.clearSession()

        assertTrue(store.history.first().isEmpty())
        assertNull(store.quality.first())
    }

    @Test
    fun `a record with blank fields comes back with them empty`() = runTest {
        val store = store()

        store.append(record(pingMs = 27).copy(ipAddress = "", operator = "", location = ""))

        val stored = store.history.first().single()

        assertNull(stored.ipAddress)
        assertNull(stored.operator)
        assertNull(stored.location)
    }

    @Test
    fun `the vpn flag of a record survives the store`() = runTest {
        val store = store()

        store.append(record(pingMs = 27).copy(vpnActive = true))

        assertTrue(store.history.first().single().vpnActive)
    }

    private fun record(pingMs: Int): PingRecord = PingRecord(
        timestamp = 1_754_000_000_000 + pingMs,
        ipAddress = "192.168.1.42",
        operator = "T-Mobile",
        pingMs = pingMs,
        location = "Miami, US",
        vpnActive = false,
    )
}
