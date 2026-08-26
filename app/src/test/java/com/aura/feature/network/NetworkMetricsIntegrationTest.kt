package com.aura.feature.network

import com.aura.feature.network.domain.model.PingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val SERVER_ERROR = 503

class NetworkMetricsIntegrationTest : NetworkTestCase() {

    @Test
    fun `the three metric cards come from the summary`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(
                pingMs = "27.000000",
                jitterMs = "6.000000",
                packetLossPct = "0.200000",
            ),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the summary") { it.pingMs == 27 }

        assertEquals(6, metrics.jitterMs)
        assertEquals(0.2, metrics.packetLossPercent!!, 0.0001)
    }

    @Test
    fun `before the first speed test jitter and packet loss stay empty`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(pingMs = "27.000000", jitterMs = null, packetLossPct = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the summary") { it.pingMs == 27 }

        assertNull(metrics.jitterMs)
        assertNull(metrics.packetLossPercent)
    }

    @Test
    fun `a decimal ping from the wire is cut to a whole number`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(pingMs = "27.900000"),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals(27, awaitMetrics(states, "the summary") { it.pingMs != null }.pingMs)
    }

    @Test
    fun `a decimal packet loss keeps its fraction`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(packetLossPct = "0.450000"),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the summary") { it.packetLossPercent != null }

        assertEquals(0.45, metrics.packetLossPercent!!, 0.0001)
    }

    @Test
    fun `a ping the wire cannot spell is shown as empty`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(pingMs = ""),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertNull(awaitContent(states).metrics.pingMs)
    }

    @Test
    fun `the last measurement wins over the summary`() = network { stack ->
        stack.localStore.saveSpeedTest(pingMs = 41, jitterMs = 9, packetLossPercent = 0.7)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(
                pingMs = "27.000000",
                jitterMs = "3.000000",
                packetLossPct = "0.100000",
            ),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the last measurement") { it.pingMs == 41 }

        assertEquals(9, metrics.jitterMs)
        assertEquals(0.7, metrics.packetLossPercent!!, 0.0001)
    }

    @Test
    fun `the summary fills in what was never measured here`() = network { stack ->
        stack.localStore.savePing(pingMs = 41)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(
                pingMs = "27.000000",
                jitterMs = "3.000000",
                packetLossPct = "0.100000",
            ),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the last measurement") { it.pingMs == 41 }

        assertEquals(3, metrics.jitterMs)
        assertEquals(0.1, metrics.packetLossPercent!!, 0.0001)
    }

    @Test
    fun `a failing summary leaves the last measurement on the screen`() = network { stack ->
        stack.localStore.saveSpeedTest(pingMs = 41, jitterMs = 9, packetLossPercent = 0.7)
        stack.server.always(NetworkPaths.SUMMARY, code = SERVER_ERROR, method = NetworkPaths.GET)
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the last measurement") { it.pingMs == 41 }

        assertEquals(9, metrics.jitterMs)
        assertEquals(0.7, metrics.packetLossPercent!!, 0.0001)
    }

    @Test
    fun `a failing summary with nothing measured leaves the cards empty`() = network { stack ->
        stack.server.always(NetworkPaths.SUMMARY, code = SERVER_ERROR, method = NetworkPaths.GET)
        val (_, states) = screenOf(stack)

        val metrics = awaitContent(states).metrics

        assertNull(metrics.pingMs)
        assertNull(metrics.jitterMs)
        assertNull(metrics.packetLossPercent)
    }

    @Test
    fun `a finished speed test refreshes all three cards at once`() = network { stack ->
        stack.pingProbe.sample = sampleOf(30.0, 42.0)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(pingMs = "27.000000", jitterMs = null, packetLossPct = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)
        awaitMetrics(states, "the summary") { it.pingMs == 27 }

        stack.speedTestEngine.start()

        val metrics = awaitMetrics(states, "the measured ping") { it.pingMs == 30 }

        assertEquals(6, metrics.jitterMs)
        assertEquals(0.0, metrics.packetLossPercent!!, 0.0001)
    }

    @Test
    fun `a probe refreshes the ping card without touching the rest`() = network { stack ->
        stack.pingProbe.measurement = 41
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(pingMs = "27.000000", jitterMs = "3.000000"),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)
        awaitMetrics(states, "the summary") { it.pingMs == 27 }

        stack.pingHistory.recordProbe(PingSource.HOME)

        val metrics = awaitMetrics(states, "the measured ping") { it.pingMs == 41 }

        assertEquals(3, metrics.jitterMs)
    }
}
