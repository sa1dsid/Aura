package com.aura.feature.home.domain.model

enum class NetworkType {
    MOBILE_2G,
    MOBILE_3G,
    MOBILE_4G,
    MOBILE_5G,
    WIFI,
    NONE,
}

data class ConnectionState(
    val networkType: NetworkType,
    val isVpnActive: Boolean,
    val rewardIon: Int,
) {
    val isClickable: Boolean get() = isVpnActive
}
