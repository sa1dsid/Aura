package com.aura.feature.network.data.diagnostics

import com.aura.feature.home.MutableNetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocketPingProbeTest {

    private val networkMonitor = MutableNetworkMonitor()

    private val probe = SocketPingProbe(networkMonitor, Dispatchers.Unconfined)

    @Test
    fun `an offline device is never pinged`() = runTest {
        networkMonitor.set(isOnline = false)

        assertNull(probe.measure())
    }

    @Test
    fun `an offline device samples nothing and counts no attempt`() = runTest {
        networkMonitor.set(isOnline = false)

        val sample = probe.sample(attempts = 12)

        assertTrue(sample.roundTripsMs.isEmpty())
        assertEquals(0, sample.attempts)
        assertNull(sample.pingMs)
        assertEquals(0.0, sample.packetLossPercent, 0.0001)
    }
}
