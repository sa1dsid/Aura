package com.aura.feature.network.data.mapper

import com.aura.core.api.dto.PingDto
import com.aura.core.network.NetworkType
import com.aura.feature.network.data.remote.dto.NetworkSnapshotDto
import com.aura.feature.network.domain.model.IpProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkMapperTest {

    @Test
    fun `a connection type is read by its own name`() {
        assertEquals(
            NetworkType.MOBILE_5G,
            snapshot(networkType = "MOBILE_5G").toDomain(isVpnActive = false).networkType,
        )
    }

    @Test
    fun `a connection type nobody knows becomes none`() {
        assertEquals(
            NetworkType.NONE,
            snapshot(networkType = "SATELLITE").toDomain(isVpnActive = false).networkType,
        )
    }

    @Test
    fun `a lowercase connection type is not recognised`() {
        assertEquals(
            NetworkType.NONE,
            snapshot(networkType = "wifi").toDomain(isVpnActive = false).networkType,
        )
    }

    @Test
    fun `the protocol name is read whatever its case`() {
        assertEquals(
            IpProtocol.IPV6,
            snapshot(protocol = "ipv6").toDomain(isVpnActive = false).protocol,
        )
    }

    @Test
    fun `a protocol nobody knows falls back to ipv4`() {
        assertEquals(
            IpProtocol.IPV4,
            snapshot(protocol = "quic").toDomain(isVpnActive = false).protocol,
        )
    }

    @Test
    fun `no protocol stays no protocol`() {
        assertNull(snapshot(protocol = null).toDomain(isVpnActive = false).protocol)
    }

    @Test
    fun `the vpn flag is taken from the system and not from the snapshot`() {
        assertTrue(snapshot().toDomain(isVpnActive = true).isVpnActive)
        assertFalse(snapshot().toDomain(isVpnActive = false).isVpnActive)
    }

    @Test
    fun `the card is clickable exactly when the vpn is up`() {
        assertTrue(snapshot().toDomain(isVpnActive = true).isVpnCardClickable)
        assertFalse(snapshot().toDomain(isVpnActive = false).isVpnCardClickable)
    }

    @Test
    fun `decimal metrics are cut to whole milliseconds`() {
        val metrics = snapshot(pingMs = "27.900000", jitterMs = "6.800000").toMetrics()

        assertEquals(27, metrics.pingMs)
        assertEquals(6, metrics.jitterMs)
    }

    @Test
    fun `packet loss keeps its fraction`() {
        assertEquals(0.45, snapshot(packetLossPercent = "0.450000").toMetrics().packetLossPercent!!, 0.0001)
    }

    @Test
    fun `metrics the wire cannot spell come out empty`() {
        val metrics = snapshot(pingMs = "", jitterMs = "n a", packetLossPercent = "-").toMetrics()

        assertNull(metrics.pingMs)
        assertNull(metrics.jitterMs)
        assertNull(metrics.packetLossPercent)
    }

    @Test
    fun `missing metrics come out empty`() {
        val metrics = snapshot(pingMs = null, jitterMs = null, packetLossPercent = null).toMetrics()

        assertNull(metrics.pingMs)
        assertNull(metrics.jitterMs)
        assertNull(metrics.packetLossPercent)
    }

    @Test
    fun `a measurement carries every field the log line needs`() {
        val record = measurement(vpn = true).toDomain()!!

        assertEquals("192.168.1.42", record.ipAddress)
        assertEquals("T-Mobile", record.operator)
        assertEquals("Miami, US", record.location)
        assertEquals(27, record.pingMs)
        assertTrue(record.vpnActive)
    }

    @Test
    fun `a measurement without a readable time is dropped`() {
        assertNull(measurement(measuredAt = "").toDomain())
        assertNull(measurement(measuredAt = "yesterday").toDomain())
    }

    @Test
    fun `a measurement without a readable ping is dropped`() {
        assertNull(measurement(pingMs = "").toDomain())
        assertNull(measurement(pingMs = "fast").toDomain())
    }

    @Test
    fun `a decimal ping in a measurement is cut to a whole number`() {
        assertEquals(27, measurement(pingMs = "27.900000").toDomain()!!.pingMs)
    }

    @Test
    fun `a measurement keeps blank fields blank instead of empty`() {
        val record = measurement(ip = "", operator = "", location = "").toDomain()!!

        assertEquals("", record.ipAddress)
        assertEquals("", record.operator)
        assertEquals("", record.location)
    }

    @Test
    fun `a measurement keeps missing fields empty`() {
        val record = measurement(ip = null, operator = null, location = null).toDomain()!!

        assertNull(record.ipAddress)
        assertNull(record.operator)
        assertNull(record.location)
    }

    private fun snapshot(
        networkType: String = "WIFI",
        operator: String? = "T-Mobile",
        ipAddress: String? = "192.168.1.42",
        protocol: String? = "IPv4",
        location: String? = "Miami, US",
        lastTestedAt: String? = null,
        pingMs: String? = "27.000000",
        jitterMs: String? = null,
        packetLossPercent: String? = null,
    ) = NetworkSnapshotDto(
        networkType = networkType,
        operator = operator,
        ipAddress = ipAddress,
        protocol = protocol,
        location = location,
        lastTestedAt = lastTestedAt,
        pingMs = pingMs,
        jitterMs = jitterMs,
        packetLossPercent = packetLossPercent,
    )

    private fun measurement(
        ip: String? = "192.168.1.42",
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
}
