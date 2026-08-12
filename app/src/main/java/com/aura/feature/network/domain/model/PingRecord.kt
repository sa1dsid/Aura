package com.aura.feature.network.domain.model

const val PING_HISTORY_LIMIT = 50

const val PING_IDEAL_MS = 20

const val PING_GOOD_MS = 30

const val PING_WARNING_MS = 45

data class PingRecord(
    val timestamp: Long,
    val ipAddress: String?,
    val operator: String?,
    val pingMs: Int,
    val location: String?,
    val vpnActive: Boolean,
)
