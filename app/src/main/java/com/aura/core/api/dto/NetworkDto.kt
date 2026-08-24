package com.aura.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PingCreateDto(
    val source: String,
    val operator: String? = null,
    val connection: String? = null,
    val protocol: String? = null,
    val vpn: Boolean = false,
    @SerialName("ping_ms") val pingMs: Double,
    @SerialName("jitter_ms") val jitterMs: Double? = null,
    @SerialName("packet_loss_pct") val packetLossPct: Double? = null,
    @SerialName("download_mbps") val downloadMbps: Double? = null,
    @SerialName("upload_mbps") val uploadMbps: Double? = null,
)

@Serializable
data class PingDto(
    val id: Int,
    val ip: String? = null,
    val operator: String? = null,
    val location: String? = null,
    val connection: String? = null,
    val protocol: String? = null,
    val vpn: Boolean = false,
    @SerialName("ping_ms") val pingMs: String = "0",
    @SerialName("jitter_ms") val jitterMs: String? = null,
    @SerialName("packet_loss_pct") val packetLossPct: String? = null,
    @SerialName("download_mbps") val downloadMbps: String? = null,
    @SerialName("upload_mbps") val uploadMbps: String? = null,
    val score: String? = null,
    @SerialName("measured_at") val measuredAt: String,
)

@Serializable
data class NetworkStateUpdateDto(
    val operator: String? = null,
    val connection: String? = null,
    val protocol: String? = null,
    val vpn: Boolean = false,
)

@Serializable
data class NetworkStateDto(
    val ip: String? = null,
    val operator: String? = null,
    val location: String? = null,
    val connection: String? = null,
    val protocol: String? = null,
    val vpn: Boolean? = null,
)

@Serializable
data class NetworkSummaryDto(
    val ip: String? = null,
    val operator: String? = null,
    val location: String? = null,
    val connection: String? = null,
    val protocol: String? = null,
    val vpn: Boolean? = null,
    @SerialName("last_tested_at") val lastTestedAt: String? = null,
    @SerialName("ping_ms") val pingMs: String? = null,
    @SerialName("jitter_ms") val jitterMs: String? = null,
    @SerialName("packet_loss_pct") val packetLossPct: String? = null,
)
