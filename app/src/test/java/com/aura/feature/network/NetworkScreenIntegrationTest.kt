package com.aura.feature.network

import com.aura.core.common.parseIsoMillis
import com.aura.core.network.NetworkType
import com.aura.feature.network.presentation.NetworkUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SERVER_ERROR = 503

class NetworkScreenIntegrationTest : NetworkTestCase() {

    @Test
    fun `the screen starts on loading`() = network { stack ->
        val (_, states) = screenOf(stack)

        awaitContent(states)

        assertEquals(NetworkUiState.Loading, states.first())
    }

    @Test
    fun `opening the screen syncs the state and asks for the summary`() = network { stack ->
        val (_, states) = screenOf(stack)
        awaitContent(states)

        assertEquals(1, stack.server.hits(NetworkPaths.STATE, NetworkPaths.PUT))
        assertEquals(1, stack.server.hits(NetworkPaths.SUMMARY, NetworkPaths.GET))
    }

    @Test
    fun `opening the screen loads the journal`() = network { stack ->
        screenOf(stack)

        awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.GET)
    }

    @Test
    fun `the state is synced before the summary and the journal are asked for`() =
        network { stack ->
            screenOf(stack)
            awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.GET)

            assertEquals(
                listOf(NetworkPaths.STATE, NetworkPaths.SUMMARY, NetworkPaths.MEASUREMENTS),
                stack.server.paths(),
            )
        }

    @Test
    fun `coming back to the screen asks the server again`() = network { stack ->
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        viewModel.onScreenResumed()

        awaitRequest(stack, NetworkPaths.SUMMARY, NetworkPaths.GET, count = 2)
        awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.GET, count = 2)
    }

    @Test
    fun `a change of network refreshes the screen by itself`() = network { stack ->
        val (_, states) = screenOf(stack)
        awaitContent(states)

        stack.networkMonitor.set(type = NetworkType.MOBILE_5G)

        awaitRequest(stack, NetworkPaths.SUMMARY, NetworkPaths.GET, count = 2)
    }

    @Test
    fun `the very first network status does not count as a change`() = network { stack ->
        val (_, states) = screenOf(stack)
        awaitContent(states)
        awaitRequest(stack, NetworkPaths.MEASUREMENTS, NetworkPaths.GET)

        assertEquals(1, stack.server.hits(NetworkPaths.SUMMARY, NetworkPaths.GET))
    }

    @Test
    fun `a failing state sync still lets the summary through`() = network { stack ->
        stack.server.always(NetworkPaths.STATE, code = SERVER_ERROR, method = NetworkPaths.PUT)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(ip = Net.IPV6, pingMs = "31.000000"),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val content = awaitContent(states)

        assertEquals(Net.IPV6, content.connection.ipAddress)
        assertEquals(31, content.metrics.pingMs)
    }

    @Test
    fun `a failing summary keeps what the state sync brought`() = network { stack ->
        stack.server.always(NetworkPaths.SUMMARY, code = SERVER_ERROR, method = NetworkPaths.GET)
        val (_, states) = screenOf(stack)

        val content = awaitContent(states)

        assertEquals(Net.IPV4, content.connection.ipAddress)
        assertEquals(Net.LOCATION, content.connection.location)
        assertNull(content.metrics.pingMs)
    }

    @Test
    fun `both handles failing leave the screen on loading`() = network { stack ->
        stack.server.always(NetworkPaths.STATE, code = SERVER_ERROR, method = NetworkPaths.PUT)
        stack.server.always(NetworkPaths.SUMMARY, code = SERVER_ERROR, method = NetworkPaths.GET)
        val (_, states) = screenOf(stack)

        awaitRequest(stack, NetworkPaths.SUMMARY, NetworkPaths.GET)

        assertTrue(states.none { it is NetworkUiState.Content })
    }

    @Test
    fun `a failing refresh keeps what is already on the screen`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(operator = Net.OTHER_OPERATOR),
            method = NetworkPaths.GET,
        )
        val (viewModel, states) = screenOf(stack)
        assertEquals(Net.OTHER_OPERATOR, awaitContent(states).connection.operator)

        stack.server.always(NetworkPaths.STATE, code = SERVER_ERROR, method = NetworkPaths.PUT)
        stack.server.always(NetworkPaths.SUMMARY, code = SERVER_ERROR, method = NetworkPaths.GET)
        viewModel.onScreenResumed()
        awaitRequest(stack, NetworkPaths.SUMMARY, NetworkPaths.GET, count = 2)

        assertEquals(Net.OTHER_OPERATOR, awaitContent(states).connection.operator)
    }

    @Test
    fun `a dropped connection is retried and the summary still arrives`() = network { stack ->
        stack.server.nextDropsConnection(NetworkPaths.SUMMARY, NetworkPaths.GET)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(pingMs = "44.000000"),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals(44, awaitMetrics(states, "the summary") { it.pingMs == 44 }.pingMs)
        assertTrue(stack.server.hits(NetworkPaths.SUMMARY, NetworkPaths.GET) >= 2)
    }

    @Test
    fun `the handle comes from the open session`() = network { stack ->
        val (_, states) = screenOf(stack)

        assertEquals("syrex", awaitContent(states).handle)
    }

    @Test
    fun `a later sign in reaches the header without a reload`() =
        network(signedIn = false) { stack ->
            val (_, states) = screenOf(stack)
            assertNull(awaitContent(states).handle)

            stack.signIn(handle = "nova")

            awaitScreen(states, "the handle to appear") { it.handle == "nova" }
        }

    @Test
    fun `without a session the header has no handle`() = network(signedIn = false) { stack ->
        val (_, states) = screenOf(stack)

        assertNull(awaitContent(states).handle)
    }

    @Test
    fun `closing the session drops the cached snapshot`() = network { stack ->
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        stack.repository.clearSession()
        val fresh = stack.eventsOf(stack.repository.observeConnection())

        assertTrue(fresh.isEmpty())
        assertTrue(states.last() is NetworkUiState.Content)

        viewModel.onScreenResumed()

        awaitEvent(fresh)
    }

    @Test
    fun `closing the session empties the journal`() = network { stack ->
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.journal(count = 3),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)
        assertEquals(3, awaitHistory(states, "the journal") { it.size == 3 }.size)

        stack.localStore.clearSession()

        awaitScreen(states, "the journal to be emptied") { it.history.isEmpty() }
    }

    @Test
    fun `unread news lights up the planet in the top bar`() = network(news = unreadNews()) { stack ->
        val (_, states) = screenOf(stack)

        assertTrue(awaitContent(states).hasUnreadNews)
    }

    @Test
    fun `reading the news puts the planet out`() = network(news = unreadNews()) { stack ->
        val (_, states) = screenOf(stack)
        assertTrue(awaitContent(states).hasUnreadNews)

        stack.newsRepository.markAllRead()

        awaitScreen(states, "the planet to go out") { !it.hasUnreadNews }
    }

    @Test
    fun `without news the planet stays dark`() = network { stack ->
        val (_, states) = screenOf(stack)

        assertFalse(awaitContent(states).hasUnreadNews)
    }

    @Test
    fun `last tested at is read from the journal and not from the summary`() = network { stack ->
        val measuredAt = Net.isoAt(day = 20, hour = 21, minute = 47)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(lastTestedAt = Net.MEASURED_AT),
            method = NetworkPaths.GET,
        )
        stack.server.always(
            NetworkPaths.MEASUREMENTS,
            body = Net.list(listOf(Net.ping(measuredAt = measuredAt))),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val content = awaitScreen(states, "the journal") { it.history.isNotEmpty() }

        assertEquals(measuredAt.parseIsoMillis(), content.lastTestedAt)
    }

    @Test
    fun `an empty journal leaves the screen without a last tested time`() = network { stack ->
        val (_, states) = screenOf(stack)

        assertNull(awaitContent(states).lastTestedAt)
    }
}
