package com.aura.feature.network.data.mapper

import com.aura.core.api.dto.NetworkStateDto
import com.aura.core.api.dto.NetworkSummaryDto
import com.aura.core.api.dto.PingDto
import com.aura.core.common.parseIsoMillis
import com.aura.core.network.NetworkType
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.IpProtocol
import com.aura.feature.network.domain.model.LinkConditions
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.model.PingRecord

fun NetworkStateDto.toConnection(conditions: LinkConditions): ConnectionDetails = connectionOf(
    ip = ip,
    operator = operator,
    protocol = protocol,
    location = location,
    conditions = conditions,
)

fun NetworkSummaryDto.toConnection(conditions: LinkConditions): ConnectionDetails = connectionOf(
    ip = ip,
    operator = operator,
    protocol = protocol,
    location = location,
    conditions = conditions,
)

fun NetworkSummaryDto.toMetrics(): NetworkMetrics = NetworkMetrics(
    pingMs = pingMs?.toDoubleOrNull()?.toInt(),
    jitterMs = jitterMs?.toDoubleOrNull()?.toInt(),
    packetLossPercent = packetLossPct?.toDoubleOrNull(),
)

fun PingDto.toRecord(): PingRecord? {
    val timestamp = measuredAt.parseIsoMillis() ?: return null
    val ping = pingMs.toDoubleOrNull()?.toInt() ?: return null

    return PingRecord(
        timestamp = timestamp,
        ipAddress = ip.orNull(),
        operator = operator.orNull(),
        pingMs = ping,
        location = location.orNull(),
        vpnActive = vpn,
    )
}

fun protocolOf(ip: String?, named: String?): IpProtocol? = when {
    !ip.isNullOrBlank() -> if (ip.contains(':')) IpProtocol.IPV6 else IpProtocol.IPV4
    else -> named?.toProtocol()
}

fun NetworkType.toWire(): String? = if (this == NetworkType.NONE) null else name

fun IpProtocol.toWire(): String = when (this) {
    IpProtocol.IPV4 -> "IPv4"
    IpProtocol.IPV6 -> "IPv6"
}

private fun connectionOf(
    ip: String?,
    operator: String?,
    protocol: String?,
    location: String?,
    conditions: LinkConditions,
): ConnectionDetails = ConnectionDetails(
    networkType = conditions.networkType,
    operator = operator ?: conditions.operator,
    ipAddress = ip,
    protocol = protocolOf(ip, protocol),
    location = location,
    isVpnActive = conditions.isVpnActive,
)

private fun String.toProtocol(): IpProtocol? =
    IpProtocol.entries.firstOrNull { it.toWire().equals(trim(), ignoreCase = true) }

private fun String?.orNull(): String? = this?.takeIf(String::isNotBlank)
