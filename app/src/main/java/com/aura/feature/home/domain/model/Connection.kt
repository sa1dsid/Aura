package com.aura.feature.home.domain.model

enum class NetworkType(val label: String) {
    MOBILE_2G("Mobile 2G"),
    MOBILE_3G("Mobile 3G"),
    MOBILE_4G("Mobile 4G"),
    MOBILE_5G("Mobile 5G"),
    WIFI("Wi-Fi"),
    NONE("No network"),
}

data class ConnectionState(
    val networkType: NetworkType,
    val isVpnActive: Boolean,
    val rewardIon: Int,
) {
    val isClickable: Boolean get() = isVpnActive
}
