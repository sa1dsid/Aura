package com.aura.feature.network.data.mapper

import com.aura.core.api.dto.NetworkStateDto
import com.aura.core.api.dto.NetworkSummaryDto
import com.aura.core.api.dto.PingDto
import com.aura.core.network.NetworkType
import com.aura.feature.network.domain.model.IpProtocol
import com.aura.feature.network.domain.model.LinkConditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMapperTest {

    @Test
    fun `the connection type is taken from the link and not from the wire`() {
        val connection = summary(connection = "WIFI")
            .toConnection(conditions(networkType = NetworkType.MOBILE_5G))

        assertEquals(NetworkType.MOBILE_5G, connection.networkType)
    }

    @Test
    fun `the vpn flag is taken from the link and not from the wire`() {
        assertTrue(summary(vpn = false).toConnection(conditions(isVpnActive = true)).isVpnActive)
        assertFalse(summary(vpn = true).toConnection(conditions(isVpnActive = false)).isVpnActive)
    }

    @Test
    fun `the operator from the wire wins over the one the link knows`() {
        val connection = summary(operator = "Vodafone")
            .toConnection(conditions(operator = "T-Mobile"))

        assertEquals("Vodafone", connection.operator)
    }

    @Test
    fun `without an operator on the wire the one the link knows is used`() {
        val connection = summary(operator = null).toConnection(conditions(operator = "T-Mobile"))

        assertEquals("T-Mobile", connection.operator)
    }

    @Test
    fun `with no operator anywhere the card stays empty`() {
        assertNull(summary(operator = null).toConnection(conditions(operator = null)).operator)
    }

    @Test
    fun `a state answer is read the same way as a summary`() {
        val connection = state(ip = IPV6, operator = null)
            .toConnection(conditions(operator = "T-Mobile"))

        assertEquals(IPV6, connection.ipAddress)
        assertEquals(IpProtocol.IPV6, connection.protocol)
        assertEquals("T-Mobile", connection.operator)
    }

    @Test
    fun `an address with dots is ipv4`() {
        assertEquals(IpProtocol.IPV4, protocolOf(ip = IPV4, named = null))
    }

    @Test
    fun `an address with colons is ipv6`() {
        assertEquals(IpProtocol.IPV6, protocolOf(ip = IPV6, named = null))
    }

    @Test
    fun `the address wins over the protocol the wire names`() {
        assertEquals(IpProtocol.IPV4, protocolOf(ip = IPV4, named = "IPv6"))
    }

    @Test
    fun `without an address the named protocol is read whatever its case`() {
        assertEquals(IpProtocol.IPV6, protocolOf(ip = null, named = "ipv6"))
        assertEquals(IpProtocol.IPV4, protocolOf(ip = null, named = "IPV4"))
    }

    @Test
    fun `a blank address is the same as no address`() {
        assertEquals(IpProtocol.IPV6, protocolOf(ip = "", named = "IPv6"))
    }

    @Test
    fun `a protocol nobody knows is no protocol at all`() {
        assertNull(protocolOf(ip = null, named = "quic"))
    }

    @Test
    fun `with neither an address nor a named protocol there is no protocol`() {
        assertNull(protocolOf(ip = null, named = null))
    }

    @Test
    fun `decimal metrics are cut to whole milliseconds`() {
        val metrics = summary(pingMs = "27.900000", jitterMs = "6.800000").toMetrics()

        assertEquals(27, metrics.pingMs)
        assertEquals(6, metrics.jitterMs)
    }

    @Test
    fun `packet loss keeps its fraction`() {
        assertEquals(0.45, summary(packetLossPct = "0.450000").toMetrics().packetLossPercent!!, 0.0001)
    }

    @Test
    fun `metrics the wire cannot spell come out empty`() {
        val metrics = summary(pingMs = "", jitterMs = "n a", packetLossPct = "-").toMetrics()

        assertNull(metrics.pingMs)
        assertNull(metrics.jitterMs)
        assertNull(metrics.packetLossPercent)
    }

    @Test
    fun `missing metrics come out empty`() {
        val metrics = summary(pingMs = null, jitterMs = null, packetLossPct = null).toMetrics()

        assertNull(metrics.pingMs)
        assertNull(metrics.jitterMs)
        assertNull(metrics.packetLossPercent)
    }

    @Test
    fun `a measurement carries every field the log line needs`() {
        val record = measurement(vpn = true).toRecord()!!

        assertEquals(IPV4, record.ipAddress)
        assertEquals("T-Mobile", record.operator)
        assertEquals("Miami, US", record.location)
        assertEquals(27, record.pingMs)
        assertTrue(record.vpnActive)
    }

    @Test
    fun `a measurement without a readable time is dropped`() {
        assertNull(measurement(measuredAt = "").toRecord())
        assertNull(measurement(measuredAt = "yesterday").toRecord())
    }

    @Test
    fun `a measurement without a readable ping is dropped`() {
        assertNull(measurement(pingMs = "").toRecord())
        assertNull(measurement(pingMs = "fast").toRecord())
    }

    @Test
    fun `a decimal ping in a measurement is cut to a whole number`() {
        assertEquals(27, measurement(pingMs = "27.900000").toRecord()!!.pingMs)
    }

    @Test
    fun `a measurement with blank fields comes out with them empty`() {
        val record = measurement(ip = "", operator = " ", location = "").toRecord()!!

        assertNull(record.ipAddress)
        assertNull(record.operator)
        assertNull(record.location)
    }

    @Test
    fun `a measurement with missing fields comes out with them empty`() {
        val record = measurement(ip = null, operator = null, location = null).toRecord()!!

        assertNull(record.ipAddress)
        assertNull(record.operator)
        assertNull(record.location)
    }

    @Test
    fun `no network goes on the wire as no connection at all`() {
        assertNull(NetworkType.NONE.toWire())
    }

    @Test
    fun `a known network goes on the wire by its own name`() {
        assertEquals("MOBILE_4G", NetworkType.MOBILE_4G.toWire())
        assertEquals("WIFI", NetworkType.WIFI.toWire())
    }

    @Test
    fun `a protocol goes on the wire the way the server spells it`() {
        assertEquals("IPv4", IpProtocol.IPV4.toWire())
        assertEquals("IPv6", IpProtocol.IPV6.toWire())
    }

    @Test
    fun `what goes on the wire comes back as the same protocol`() {
        IpProtocol.entries.forEach { protocol ->
            assertEquals(protocol, protocolOf(ip = null, named = protocol.toWire()))
        }
    }

    private fun conditions(
        networkType: NetworkType = NetworkType.WIFI,
        operator: String? = "T-Mobile",
        protocol: IpProtocol? = null,
        isVpnActive: Boolean = false,
    ) = LinkConditions(
        networkType = networkType,
        operator = operator,
        protocol = protocol,
        isVpnActive = isVpnActive,
    )

    private fun state(
        ip: String? = IPV4,
        operator: String? = "T-Mobile",
        location: String? = "Miami, US",
        connection: String? = "WIFI",
        protocol: String? = null,
        vpn: Boolean = false,
    ) = NetworkStateDto(
        ip = ip,
        operator = operator,
        location = location,
        connection = connection,
        protocol = protocol,
        vpn = vpn,
    )

    private fun summary(
        ip: String? = IPV4,
        operator: String? = "T-Mobile",
        location: String? = "Miami, US",
        connection: String? = "WIFI",
        protocol: String? = null,
        vpn: Boolean = false,
        pingMs: String? = "27.000000",
        jitterMs: String? = null,
        packetLossPct: String? = null,
    ) = NetworkSummaryDto(
        ip = ip,
        operator = operator,
        location = location,
        connection = connection,
        protocol = protocol,
        vpn = vpn,
        pingMs = pingMs,
        jitterMs = jitterMs,
        packetLossPct = packetLossPct,
    )

    private fun measurement(
        ip: String? = IPV4,
        operator: String? = "T-Mobile",
        location: String? = "Miami, US",
        vpn: Boolean = false,
        pingMs: String = "27.000000",
        measuredAt: String = "2026-08-26T08:14:00.000Z",
    ) = PingDto(
        id = 1,
        ip = ip,
        operator = operator,
        location = location,
        vpn = vpn,
        pingMs = pingMs,
        measuredAt = measuredAt,
    )

    private companion object {
        const val IPV4 = "192.168.1.42"
        const val IPV6 = "2a02:6b8:c02:901:0:fc00:1:2"
    }
}
