package com.aura.feature.network.presentation

import androidx.compose.runtime.Immutable
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.SpeedTestFailure
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.model.SpeedTestState

@Immutable
sealed interface NetworkUiState {

    data object Loading : NetworkUiState

    data class Content(
        val handle: String?,
        val connection: ConnectionDetails,
        val metrics: NetworkMetrics,
        val history: List<PingRecord>,
        val diagnostics: SpeedTestState,
        val hasUnreadNews: Boolean,
    ) : NetworkUiState {
        val lastTestedAt: Long? get() = history.lastOrNull()?.timestamp
    }
}

sealed interface NetworkEvent {

    data class DiagnosticsFailed(val failure: SpeedTestFailure) : NetworkEvent

    data class ShareResult(val result: SpeedTestResult) : NetworkEvent

    data object OpenVpnSettings : NetworkEvent
}

@Immutable
data class NetworkActions(
    val onMenuClick: () -> Unit = {},
    val onNewsClick: () -> Unit = {},
    val onVpnCardClick: () -> Unit = {},
    val onStartTestClick: () -> Unit = {},
    val onShareResultClick: () -> Unit = {},
)
