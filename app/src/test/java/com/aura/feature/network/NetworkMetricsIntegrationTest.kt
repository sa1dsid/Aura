package com.aura.feature.network

import com.aura.feature.network.data.local.MeasuredQuality
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
    fun `the locally measured jitter fills in while the server is silent`() = network { stack ->
        stack.localStore.saveQuality(MeasuredQuality(jitterMs = 9, packetLossPercent = 0.7))
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(jitterMs = null, packetLossPct = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the local quality") { it.jitterMs == 9 }

        assertEquals(0.7, metrics.packetLossPercent!!, 0.0001)
    }

    @Test
    fun `the server jitter wins over the locally measured one`() = network { stack ->
        stack.localStore.saveQuality(MeasuredQuality(jitterMs = 9, packetLossPercent = 0.7))
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(jitterMs = "3.000000", packetLossPct = "0.100000"),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the summary") { it.jitterMs == 3 }

        assertEquals(0.1, metrics.packetLossPercent!!, 0.0001)
    }

    @Test
    fun `the locally measured quality never fills in for the ping`() = network { stack ->
        stack.localStore.saveQuality(MeasuredQuality(jitterMs = 9, packetLossPercent = 0.7))
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(pingMs = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the local quality") { it.jitterMs == 9 }

        assertNull(metrics.pingMs)
    }

    @Test
    fun `a failing summary empties the ping but keeps the local quality`() = network { stack ->
        stack.localStore.saveQuality(MeasuredQuality(jitterMs = 9, packetLossPercent = 0.7))
        stack.server.always(NetworkPaths.SUMMARY, code = SERVER_ERROR, method = NetworkPaths.GET)
        val (_, states) = screenOf(stack)

        val metrics = awaitMetrics(states, "the local quality") { it.jitterMs == 9 }

        assertNull(metrics.pingMs)
    }

    @Test
    fun `a finished speed test refreshes the jitter and the packet loss on the screen`() =
        network { stack ->
            stack.pingProbe.sample = sampleOf(30.0, 42.0)
            stack.server.always(
                NetworkPaths.SUMMARY,
                body = Net.summary(pingMs = "27.000000", jitterMs = null, packetLossPct = null),
                method = NetworkPaths.GET,
            )
            val (_, states) = screenOf(stack)
            awaitMetrics(states, "the summary") { it.pingMs == 27 }

            stack.speedTestEngine.start()

            assertEquals(6, awaitMetrics(states, "the measured jitter") { it.jitterMs == 6 }.jitterMs)
        }

    @Test
    fun `a finished speed test leaves the ping card waiting for the server`() = network { stack ->
        stack.pingProbe.sample = sampleOf(90.0, 94.0)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(pingMs = "27.000000", jitterMs = null, packetLossPct = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)
        awaitMetrics(states, "the summary") { it.pingMs == 27 }

        stack.speedTestEngine.start()
        val metrics = awaitMetrics(states, "the measured jitter") { it.jitterMs != null }

        assertEquals(27, metrics.pingMs)
    }
}
