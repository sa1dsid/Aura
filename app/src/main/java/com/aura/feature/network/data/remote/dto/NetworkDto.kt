package com.aura.feature.network.data.remote.dto

data class NetworkSnapshotDto(
    val networkType: String,
    val operator: String?,
    val ipAddress: String?,
    val protocol: String?,
    val location: String?,
    val lastTestedAt: String?,
    val pingMs: String?,
    val jitterMs: String?,
    val packetLossPercent: String?,
)
