package com.aura.feature.network.presentation.preview

import com.aura.core.network.NetworkType
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.ConnectionGrade
import com.aura.feature.network.domain.model.IpProtocol
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.model.SpeedTestState
import com.aura.feature.network.presentation.NetworkUiState

private const val HOUR_MILLIS = 60L * 60 * 1000

private const val FIRST_RECORD_AT = 1_784_000_000_000L

private val PING_SAMPLE = listOf(
    24, 19, 21, 27, 33, 22, 18, 26, 41, 29,
    23, 20, 25, 31, 47, 28, 22, 19, 24, 36,
    21, 26, 30, 23, 18, 27, 44, 25, 22, 20,
    29, 34, 21, 19, 26, 23, 38, 27, 24, 21,
    25, 30, 22, 18, 28, 45, 26, 23, 20, 24,
)

private val VPN_INDICES = setOf(8, 14, 26, 45)

object NetworkPreviewData {

    private val history = PING_SAMPLE.mapIndexed { index, pingMs ->
        PingRecord(
            timestamp = FIRST_RECORD_AT + index * HOUR_MILLIS,
            ipAddress = "192.168.1.42",
            operator = "T-Mobile",
            pingMs = pingMs,
            location = "London, UK",
            vpnActive = index in VPN_INDICES,
        )
    }

    val content = NetworkUiState.Content(
        handle = "@syrex",
        connection = ConnectionDetails(
            networkType = NetworkType.MOBILE_4G,
            operator = "T-Mobile",
            ipAddress = "192.168.1.42",
            protocol = IpProtocol.IPV4,
            location = "London, UK",
            isVpnActive = false,
        ),
        metrics = NetworkMetrics(pingMs = 24, jitterMs = 6, packetLossPercent = 0.2),
        history = history,
        diagnostics = SpeedTestState.Idle,
        hasUnreadNews = false,
    )

    val vpnOnWithResult = content.copy(
        connection = content.connection.copy(isVpnActive = true),
        diagnostics = SpeedTestState.Done(
            SpeedTestResult(
                downloadMbps = 48.6,
                uploadMbps = 12.4,
                pingMs = 24,
                jitterMs = 6,
                packetLossPercent = 0.2,
                grade = ConnectionGrade.EXCELLENT,
            )
        ),
    )

    val poorResult = content.copy(
        diagnostics = SpeedTestState.Done(
            SpeedTestResult(
                downloadMbps = 2.4,
                uploadMbps = 0.8,
                pingMs = 92,
                jitterMs = 44,
                packetLossPercent = 6.0,
                grade = ConnectionGrade.POOR,
            )
        ),
    )

    val testing = content.copy(diagnostics = SpeedTestState.Running(0.45f))

    val empty = content.copy(
        metrics = NetworkMetrics(pingMs = null, jitterMs = null, packetLossPercent = null),
        history = emptyList(),
    )
}
