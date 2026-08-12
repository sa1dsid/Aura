package com.aura.feature.network.data.remote.dto

data class NetworkSnapshotDto(
    val networkType: String,
    val operator: String?,
    val ipAddress: String?,
    val ipV6: Boolean,
    val city: String?,
    val countryCode: String?,
)
