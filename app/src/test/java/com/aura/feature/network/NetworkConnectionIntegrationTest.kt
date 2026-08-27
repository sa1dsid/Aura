package com.aura.feature.network

import com.aura.core.network.NetworkType
import com.aura.feature.network.domain.model.IpProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkConnectionIntegrationTest : NetworkTestCase() {

    @Test
    fun `the connection type comes from the system and not from the server`() = network { stack ->
        stack.networkMonitor.set(type = NetworkType.MOBILE_5G)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(connection = "WIFI"),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals(NetworkType.MOBILE_5G, awaitContent(states).connection.networkType)
    }

    @Test
    fun `a connection type the system does not know is shown as none`() = network { stack ->
        stack.networkMonitor.set(type = NetworkType.NONE)
        val (_, states) = screenOf(stack)

        assertEquals(NetworkType.NONE, awaitContent(states).connection.networkType)
    }

    @Test
    fun `a network switched mid session reaches the card`() = network { stack ->
        val (_, states) = screenOf(stack)
        assertEquals(NetworkType.WIFI, awaitContent(states).connection.networkType)

        stack.networkMonitor.set(type = NetworkType.MOBILE_4G)

        awaitConnection(states, "the new network") { it.networkType == NetworkType.MOBILE_4G }
    }

    @Test
    fun `the operator from the server wins over the system one`() = network { stack ->
        stack.networkMonitor.set(operator = Net.OPERATOR)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(operator = Net.OTHER_OPERATOR),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals(Net.OTHER_OPERATOR, awaitContent(states).connection.operator)
    }

    @Test
    fun `without a server operator the system one is shown`() = network { stack ->
        stack.networkMonitor.set(operator = Net.OPERATOR)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(operator = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals(Net.OPERATOR, awaitContent(states).connection.operator)
    }

    @Test
    fun `with no operator anywhere the card stays empty`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(operator = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertNull(awaitContent(states).connection.operator)
    }

    @Test
    fun `an ipv4 address is recognised by the client itself`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(ip = Net.IPV4, protocol = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        val connection = awaitContent(states).connection

        assertEquals(Net.IPV4, connection.ipAddress)
        assertEquals(IpProtocol.IPV4, connection.protocol)
    }

    @Test
    fun `an ipv6 address is recognised by its colons`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(ip = Net.IPV6, protocol = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals(IpProtocol.IPV6, awaitContent(states).connection.protocol)
    }

    @Test
    fun `the address wins over the protocol the server names`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(ip = Net.IPV4, protocol = Net.PROTOCOL_IPV6),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals(IpProtocol.IPV4, awaitContent(states).connection.protocol)
    }

    @Test
    fun `without an address the protocol falls back to the one the summary names`() =
        network { stack ->
            stack.server.always(
                NetworkPaths.SUMMARY,
                body = Net.summary(ip = null, protocol = Net.PROTOCOL_IPV6),
                method = NetworkPaths.GET,
            )
            val (_, states) = screenOf(stack)

            val connection = awaitContent(states).connection

            assertNull(connection.ipAddress)
            assertEquals(IpProtocol.IPV6, connection.protocol)
        }

    @Test
    fun `with neither an address nor a named protocol the card stays empty`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(ip = null, protocol = null),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertNull(awaitContent(states).connection.protocol)
    }

    @Test
    fun `the first state sync goes out without a protocol`() = network { stack ->
        val (_, states) = screenOf(stack)
        awaitContent(states)

        assertFalse(stack.server.bodyOf(NetworkPaths.STATE, NetworkPaths.PUT).contains("protocol"))
    }

    @Test
    fun `the protocol learned once is sent with the next state sync`() = network { stack ->
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        viewModel.onScreenResumed()
        awaitRequest(stack, NetworkPaths.STATE, NetworkPaths.PUT, count = 2)

        assertTrue(
            stack.server.bodyOf(NetworkPaths.STATE, NetworkPaths.PUT)
                .contains("\"protocol\":\"${Net.PROTOCOL_IPV4}\"")
        )
    }

    @Test
    fun `the state sync carries the operator and the connection`() = network { stack ->
        stack.networkMonitor.set(type = NetworkType.MOBILE_4G, operator = Net.OPERATOR)
        val (_, states) = screenOf(stack)
        awaitContent(states)

        val body = stack.server.bodyOf(NetworkPaths.STATE, NetworkPaths.PUT)

        assertTrue(body.contains("\"operator\":\"${Net.OPERATOR}\""))
        assertTrue(body.contains("\"connection\":\"MOBILE_4G\""))
    }

    @Test
    fun `no network is sent as no connection at all`() = network { stack ->
        stack.networkMonitor.set(type = NetworkType.NONE)
        val (_, states) = screenOf(stack)
        awaitContent(states)

        assertFalse(
            stack.server.bodyOf(NetworkPaths.STATE, NetworkPaths.PUT).contains("connection")
        )
    }

    @Test
    fun `the vpn flag is sent even when it is off`() = network { stack ->
        val (_, states) = screenOf(stack)
        awaitContent(states)

        assertTrue(stack.server.bodyOf(NetworkPaths.STATE, NetworkPaths.PUT).contains("\"vpn\":false"))
    }

    @Test
    fun `the vpn flag is sent when it is on`() = network { stack ->
        stack.networkMonitor.set(isVpnActive = true)
        val (_, states) = screenOf(stack)
        awaitContent(states)

        assertTrue(stack.server.bodyOf(NetworkPaths.STATE, NetworkPaths.PUT).contains("\"vpn\":true"))
    }

    @Test
    fun `the vpn card is read from the system and not from the server`() = network { stack ->
        stack.networkMonitor.set(isVpnActive = true)
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(vpn = false),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertTrue(awaitContent(states).connection.isVpnActive)
    }

    @Test
    fun `a vpn switched on mid session reaches the card`() = network { stack ->
        val (_, states) = screenOf(stack)
        assertFalse(awaitContent(states).connection.isVpnActive)

        stack.networkMonitor.set(isVpnActive = true)

        awaitConnection(states, "the vpn to light up") { it.isVpnActive }
    }

    @Test
    fun `the location on the screen comes from the summary`() = network { stack ->
        stack.server.always(
            NetworkPaths.STATE,
            body = Net.state(location = Net.LOCATION),
            method = NetworkPaths.PUT,
        )
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(location = Net.OTHER_LOCATION),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals(Net.OTHER_LOCATION, awaitContent(states).connection.location)
    }

    @Test
    fun `the location is handed to the shared geo source`() = network { stack ->
        val (_, states) = screenOf(stack)
        awaitContent(states)

        awaitUntil("the geo source to learn the city") {
            stack.userLocationSource.city.value == Net.LOCATION
        }
    }

    @Test
    fun `a blank location does not wipe the remembered city`() = network { stack ->
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)
        awaitUntil("the geo source to learn the city") {
            stack.userLocationSource.city.value == Net.LOCATION
        }

        stack.server.always(
            NetworkPaths.STATE,
            body = Net.state(location = ""),
            method = NetworkPaths.PUT,
        )
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(location = ""),
            method = NetworkPaths.GET,
        )
        viewModel.onScreenResumed()
        awaitRequest(stack, NetworkPaths.SUMMARY, NetworkPaths.GET, count = 2)

        assertEquals(Net.LOCATION, stack.userLocationSource.city.value)
    }

    @Test
    fun `a blank location still reaches the card as a blank`() = network { stack ->
        stack.server.always(
            NetworkPaths.SUMMARY,
            body = Net.summary(location = ""),
            method = NetworkPaths.GET,
        )
        val (_, states) = screenOf(stack)

        assertEquals("", awaitContent(states).connection.location)
    }
}
