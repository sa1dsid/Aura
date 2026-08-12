package com.aura.feature.network.domain.model

import com.aura.core.network.NetworkType

enum class IpProtocol { IPV4, IPV6 }

data class NetworkMetrics(
    val pingMs: Int?,
    val jitterMs: Int?,
    val packetLossPercent: Double?,
)

data class ConnectionDetails(
    val networkType: NetworkType,
    val operator: String?,
    val ipAddress: String?,
    val protocol: IpProtocol?,
    val location: String?,
    val isVpnActive: Boolean,
) {
    val isVpnCardClickable: Boolean get() = isVpnActive
}

data class NetworkState(
    val metrics: NetworkMetrics,
    val connection: ConnectionDetails,
    val lastTestedAt: Long?,
)
