package com.aura.feature.network.data.mapper

import com.aura.core.api.dto.PingDto
import com.aura.core.common.parseIsoMillis
import com.aura.core.network.NetworkType
import com.aura.feature.network.data.remote.dto.NetworkSnapshotDto
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.IpProtocol
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.model.PingRecord

private const val PROTOCOL_IPV6 = "IPV6"

fun NetworkSnapshotDto.toDomain(isVpnActive: Boolean): ConnectionDetails = ConnectionDetails(
    networkType = networkType.toNetworkType(),
    operator = operator,
    ipAddress = ipAddress,
    protocol = protocol?.toProtocol(),
    location = location,
    isVpnActive = isVpnActive,
)

fun NetworkSnapshotDto.toMetrics(): NetworkMetrics = NetworkMetrics(
    pingMs = pingMs?.toDoubleOrNull()?.toInt(),
    jitterMs = jitterMs?.toDoubleOrNull()?.toInt(),
    packetLossPercent = packetLossPercent?.toDoubleOrNull(),
)

fun NetworkSnapshotDto.lastTestedAtMillis(): Long? = lastTestedAt?.parseIsoMillis()

fun PingDto.toDomain(): PingRecord? {
    val timestamp = measuredAt.parseIsoMillis() ?: return null
    val ping = pingMs.toDoubleOrNull()?.toInt() ?: return null

    return PingRecord(
        timestamp = timestamp,
        ipAddress = ip,
        operator = operator,
        pingMs = ping,
        location = location,
        vpnActive = vpn,
    )
}

private fun String.toProtocol(): IpProtocol =
    if (uppercase() == PROTOCOL_IPV6) IpProtocol.IPV6 else IpProtocol.IPV4

private fun String.toNetworkType(): NetworkType =
    NetworkType.entries.firstOrNull { it.name == this } ?: NetworkType.NONE
