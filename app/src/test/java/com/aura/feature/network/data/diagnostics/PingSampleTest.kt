package com.aura.feature.network.data.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PingSampleTest {

    @Test
    fun `the ping is the fastest round trip of the lot`() {
        assertEquals(18, sample(31.0, 18.4, 26.0, 22.0).pingMs)
    }

    @Test
    fun `half a millisecond rounds up`() {
        assertEquals(25, sample(24.5).pingMs)
    }

    @Test
    fun `a sample nobody answered has no ping`() {
        assertNull(PingSample(roundTripsMs = emptyList(), attempts = 4).pingMs)
    }

    @Test
    fun `the jitter is how far the round trips stray from their average`() {
        assertEquals(5, sample(20.0, 30.0).jitterMs)
    }

    @Test
    fun `the jitter of a steady link is zero`() {
        assertEquals(0, sample(24.0, 24.0, 24.0).jitterMs)
    }

    @Test
    fun `a single round trip is not enough for a jitter`() {
        assertNull(sample(24.0).jitterMs)
    }

    @Test
    fun `a sample nobody answered has no jitter`() {
        assertNull(PingSample(roundTripsMs = emptyList(), attempts = 4).jitterMs)
    }

    @Test
    fun `the packet loss is the share of attempts that never came back`() {
        assertEquals(
            25.0,
            PingSample(roundTripsMs = listOf(20.0, 22.0, 24.0), attempts = 4).packetLossPercent,
            0.0001,
        )
    }

    @Test
    fun `a link that answered everything loses nothing`() {
        assertEquals(0.0, sample(20.0, 22.0, 24.0, 26.0).packetLossPercent, 0.0001)
    }

    @Test
    fun `a link that answered nothing loses everything`() {
        assertEquals(
            100.0,
            PingSample(roundTripsMs = emptyList(), attempts = 4).packetLossPercent,
            0.0001,
        )
    }

    @Test
    fun `a sample nobody even tried loses nothing`() {
        assertEquals(
            0.0,
            PingSample(roundTripsMs = emptyList(), attempts = 0).packetLossPercent,
            0.0001,
        )
    }

    private fun sample(vararg roundTripsMs: Double): PingSample =
        PingSample(roundTripsMs = roundTripsMs.toList(), attempts = roundTripsMs.size)
}
