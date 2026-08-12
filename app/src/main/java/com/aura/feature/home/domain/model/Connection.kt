package com.aura.feature.home.domain.model

import com.aura.core.network.NetworkType

data class ConnectionState(
    val networkType: NetworkType,
    val isVpnActive: Boolean,
    val rewardIon: Int,
) {
    val isClickable: Boolean get() = isVpnActive
}
