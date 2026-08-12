package com.aura.feature.network.data.mapper

import com.aura.core.network.NetworkType
import com.aura.feature.network.data.remote.dto.NetworkSnapshotDto
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.IpProtocol

fun NetworkSnapshotDto.toDomain(isVpnActive: Boolean): ConnectionDetails = ConnectionDetails(
    networkType = networkType.toNetworkType(),
    operator = operator,
    ipAddress = ipAddress,
    protocol = ipAddress?.let { if (ipV6) IpProtocol.IPV6 else IpProtocol.IPV4 },
    location = locationLabel(),
    isVpnActive = isVpnActive,
)

fun NetworkSnapshotDto.locationLabel(): String? = when {
    city == null -> countryCode
    countryCode == null -> city
    else -> "$city, $countryCode"
}

private fun String.toNetworkType(): NetworkType =
    NetworkType.entries.firstOrNull { it.name == this } ?: NetworkType.NONE
