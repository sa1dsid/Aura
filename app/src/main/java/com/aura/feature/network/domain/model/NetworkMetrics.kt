package com.aura.feature.network.domain.model

import com.aura.core.network.NetworkType

enum class IpProtocol { IPV4, IPV6 }

data class NetworkMetrics(
    val pingMs: Int?,
    val jitterMs: Int?,
    val packetLossPercent: Double?,
) {
    companion object {
        val Empty = NetworkMetrics(pingMs = null, jitterMs = null, packetLossPercent = null)
    }
}

data class ConnectionDetails(
    val networkType: NetworkType,
    val operator: String?,
    val ipAddress: String?,
    val protocol: IpProtocol?,
    val location: String?,
    val isVpnActive: Boolean,
)

data class LinkConditions(
    val networkType: NetworkType,
    val operator: String?,
    val protocol: IpProtocol?,
    val isVpnActive: Boolean,
)
