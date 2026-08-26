package com.aura.feature.network

import com.aura.core.network.NetworkType
import com.aura.feature.network.domain.model.PING_HISTORY_LIMIT
import com.aura.feature.network.domain.model.PingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SERVER_ERROR = 503

class PingHistoryIntegrationTest : NetworkTestCase() {

    @Test
    fun `the journal comes from the server`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = 3),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val history = awaitHistory(states, "the journal") { it.isNotEmpty() }

        assertEquals(listOf(20, 21, 22), history.map { it.pingMs })
    }

    @Test
    fun `a record carries everything the log line needs`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.list(listOf(Net.ping(vpn = true))),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val record = awaitHistory(states, "the journal") { it.isNotEmpty() }.single()

        assertEquals(Net.IPV4, record.ipAddress)
        assertEquals(Net.OPERATOR, record.operator)
        assertEquals(Net.LOCATION, record.location)
        assertEquals(27, record.pingMs)
        assertTrue(record.vpnActive)
    }

    @Test
    fun `an empty ip and operator are kept empty and not blank`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.list(listOf(Net.ping(ip = null, operator = null, location = null))),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val record = awaitHistory(states, "the journal") { it.isNotEmpty() }.single()

        assertNull(record.ipAddress)
        assertNull(record.operator)
        assertNull(record.location)
    }

    @Test
    fun `the journal is sorted oldest first whatever order the wire brings`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.list(
                listOf(
                    Net.ping(id = 1, pingMs = "30", measuredAt = Net.isoAt(day = 3)),
                    Net.ping(id = 2, pingMs = "10", measuredAt = Net.isoAt(day = 1)),
                    Net.ping(id = 3, pingMs = "20", measuredAt = Net.isoAt(day = 2)),
                )
            ),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val history = awaitHistory(states, "the journal") { it.size == 3 }

        assertEquals(listOf(10, 20, 30), history.map { it.pingMs })
    }

    @Test
    fun `the journal keeps only the last fifty records`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = PING_HISTORY_LIMIT + 10),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val history = awaitHistory(states, "the journal") { it.size == PING_HISTORY_LIMIT }

        assertEquals(30, history.first().pingMs)
        assertEquals(79, history.last().pingMs)
    }

    @Test
    fun `a record without a time is dropped`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.list(
                listOf(Net.ping(id = 1, measuredAt = ""), Net.ping(id = 2, pingMs = "41"))
            ),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val history = awaitHistory(states, "the journal") { it.isNotEmpty() }

        assertEquals(listOf(41), history.map { it.pingMs })
    }

    @Test
    fun `a record without a ping is dropped`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.list(
                listOf(Net.ping(id = 1, pingMs = ""), Net.ping(id = 2, pingMs = "41"))
            ),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val history = awaitHistory(states, "the journal") { it.isNotEmpty() }

        assertEquals(listOf(41), history.map { it.pingMs })
    }

    @Test
    fun `a failing journal keeps what is already shown`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = 3),
            method = NetworkPaths.GET,
        )
        val (viewModel, states) = screenOf(stack)
        awaitHistory(states, "the journal") { it.size == 3 }

        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            code = SERVER_ERROR,
            method = NetworkPaths.GET,
        )
        viewModel.onScreenResumed()
        awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.GET, count = 2)

        assertEquals(3, awaitContent(states).history.size)
    }

    @Test
    fun `an empty answer from the server empties the journal`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = 3),
            method = NetworkPaths.GET,
        )
        val (viewModel, states) = screenOf(stack)
        awaitHistory(states, "the journal") { it.size == 3 }

        stack.server.always(NetworkPaths.MEASUREMENTS, body = "[]", method = NetworkPaths.GET)
        viewModel.onScreenResumed()

        awaitScreen(states, "the journal to be emptied") { it.history.isEmpty() }
    }

    @Test
    fun `a refresh replaces the journal with what the server has`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = 3),
            method = NetworkPaths.GET,
        )
        val (viewModel, states) = screenOf(stack)
        awaitHistory(states, "the journal") { it.size == 3 }

        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.list(listOf(Net.ping(pingMs = "99"))),
            method = NetworkPaths.GET,
        )
        viewModel.onScreenResumed()

        assertEquals(listOf(99), awaitHistory(states, "the new journal") { it.size == 1 }.map { it.pingMs })
    }

    @Test
    fun `a probe from home lands in the journal`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.ping(pingMs = "33"),
            method = NetworkPaths.POST,
        )

        stack.pingHistory.recordProbe(PingSource.HOME)

        val history = awaitJournal(stack, "the probe") { it.isNotEmpty() }

        assertEquals(listOf(33), history.map { it.pingMs })
    }

    @Test
    fun `the probe body names the source it came from`() = network { stack ->
        stack.pingHistory.recordProbe(PingSource.DIAGNOSTIC)

        assertTrue(
            stack.server.bodyOf(NetworkPaths.MEASUREMENTS, NetworkPaths.POST)
                .contains("\"source\":\"diagnostic\"")
        )
    }

    @Test
    fun `the probe body carries the measured ping`() = network { stack ->
        stack.pingProbe.measurement = 41

        stack.pingHistory.recordProbe(PingSource.BACKGROUND)

        assertTrue(
            stack.server.bodyOf(NetworkPaths.MEASUREMENTS, NetworkPaths.POST)
                .contains("\"ping_ms\":41.0")
        )
    }

    @Test
    fun `the probe body carries the connection, the operator and the vpn flag`() = network { stack ->
        stack.networkMonitor.set(
            type = NetworkType.MOBILE_4G,
            operator = Net.OPERATOR,
            isVpnActive = true,
        )

        stack.pingHistory.recordProbe(PingSource.HOME)

        val body = stack.server.bodyOf(NetworkPaths.MEASUREMENTS, NetworkPaths.POST)

        assertTrue(body.contains("\"connection\":\"MOBILE_4G\""))
        assertTrue(body.contains("\"operator\":\"${Net.OPERATOR}\""))
        assertTrue(body.contains("\"vpn\":true"))
    }

    @Test
    fun `a probe before any state sync goes out without a protocol`() = network { stack ->
        stack.pingHistory.recordProbe(PingSource.HOME)

        assertFalse(
            stack.server.bodyOf(NetworkPaths.MEASUREMENTS, NetworkPaths.POST).contains("protocol")
        )
    }

    @Test
    fun `a probe after a state sync carries the protocol learned there`() = network { stack ->
        stack.repository.refresh()

        stack.pingHistory.recordProbe(PingSource.HOME)

        assertTrue(
            stack.server.bodyOf(NetworkPaths.MEASUREMENTS, NetworkPaths.POST)
                .contains("\"protocol\":\"${Net.PROTOCOL_IPV4}\"")
        )
    }

    @Test
    fun `the vpn flag of the moment is what the journal keeps`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.ping(vpn = true),
            method = NetworkPaths.POST,
        )

        stack.pingHistory.recordProbe(PingSource.HOME)

        assertTrue(awaitJournal(stack, "the probe") { it.isNotEmpty() }.single().vpnActive)
    }

    @Test
    fun `a probe that could not measure writes nothing`() = network { stack ->
        stack.pingProbe.measurement = null

        stack.pingHistory.recordProbe(PingSource.HOME)

        assertEquals(0, stack.server.hits(NetworkPaths.MEASUREMENTS, NetworkPaths.POST))
        assertTrue(awaitJournal(stack, "the empty journal") { true }.isEmpty())
    }

    @Test
    fun `a probe the server refused makes the client re-read the journal`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            code = SERVER_ERROR,
            method = NetworkPaths.POST,
        )
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = 2),
            method = NetworkPaths.GET,
        )

        stack.pingHistory.recordProbe(PingSource.HOME)

        assertEquals(1, stack.server.hits(NetworkPaths.MEASUREMENTS, NetworkPaths.GET))
        assertEquals(2, awaitJournal(stack, "the re-read journal") { it.size == 2 }.size)
    }

    @Test
    fun `a background probe the server refused is left alone`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            code = SERVER_ERROR,
            method = NetworkPaths.POST,
        )

        stack.pingHistory.recordProbe(PingSource.BACKGROUND)

        assertEquals(0, stack.server.hits(NetworkPaths.MEASUREMENTS, NetworkPaths.GET))
    }

    @Test
    fun `a reply the client cannot read makes it re-read the journal`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.ping(measuredAt = ""),
            method = NetworkPaths.POST,
        )
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = 2),
            method = NetworkPaths.GET,
        )

        stack.pingHistory.recordProbe(PingSource.HOME)

        assertEquals(2, awaitJournal(stack, "the re-read journal") { it.size == 2 }.size)
    }

    @Test
    fun `an appended record does not push the journal past fifty`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = PING_HISTORY_LIMIT),
            method = NetworkPaths.GET,
        )
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.ping(pingMs = "99", measuredAt = Net.isoAt(day = 9)),
            method = NetworkPaths.POST,
        )
        stack.pingHistory.refresh()
        awaitJournal(stack, "the seeded journal") { it.size == PING_HISTORY_LIMIT }

        stack.pingHistory.recordProbe(PingSource.HOME)

        val history = awaitJournal(stack, "the appended record") { it.lastOrNull()?.pingMs == 99 }

        assertEquals(PING_HISTORY_LIMIT, history.size)
        assertEquals(21, history.first().pingMs)
    }

    @Test
    fun `the probe asks the prober exactly once`() = network { stack ->
        stack.pingHistory.recordProbe(PingSource.HOME)

        assertEquals(1, stack.pingProbe.measures)
    }

    @Test
    fun `all three sources write into one journal without a mark`() = network { stack ->
        stack.server.next(NetworkPaths.MEASUREMENTS, body = Net.ping(pingMs = "11"), method = NetworkPaths.POST)
        stack.server.next(NetworkPaths.MEASUREMENTS, body = Net.ping(pingMs = "22"), method = NetworkPaths.POST)
        stack.server.next(NetworkPaths.MEASUREMENTS, body = Net.ping(pingMs = "33"), method = NetworkPaths.POST)

        stack.pingHistory.recordProbe(PingSource.HOME)
        stack.pingHistory.recordProbe(PingSource.DIAGNOSTIC)
        stack.pingHistory.recordProbe(PingSource.BACKGROUND)

        val history = awaitJournal(stack, "all three probes") { it.size == 3 }

        assertEquals(listOf(11, 22, 33), history.map { it.pingMs })
        assertEquals(
            listOf("home", "diagnostic", "background"),
            stack.server.bodiesOf(NetworkPaths.MEASUREMENTS, NetworkPaths.POST)
                .map { it.substringAfter("\"source\":\"").substringBefore('"') },
        )
    }
}
