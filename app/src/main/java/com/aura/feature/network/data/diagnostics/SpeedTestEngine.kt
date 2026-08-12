package com.aura.feature.network.data.diagnostics

import com.aura.core.common.ApplicationScope
import com.aura.core.network.NetworkMonitor
import com.aura.feature.network.domain.model.ConnectionGrade
import com.aura.feature.network.domain.model.ConnectionScoring
import com.aura.feature.network.domain.model.SpeedTestFailure
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.model.SpeedTestState
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

@Singleton
class SpeedTestEngine @Inject constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    private val networkMonitor: NetworkMonitor,
    private val pingHistory: PingHistoryRepository,
) {

    private val _state = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    private val _failures = MutableSharedFlow<SpeedTestFailure>(extraBufferCapacity = 1)
    val failures: SharedFlow<SpeedTestFailure> = _failures.asSharedFlow()

    fun start() {
        if (_state.value is SpeedTestState.Running) return

        if (!networkMonitor.current().isOnline) {
            _state.value = SpeedTestState.Idle
            _failures.tryEmit(SpeedTestFailure.NO_CONNECTION)
            return
        }

        scope.launch {
            _state.value = SpeedTestState.Running(0f)

            repeat(PROGRESS_STEPS) { step ->
                delay(STEP_MILLIS)
                if (!networkMonitor.current().isOnline) {
                    _state.value = SpeedTestState.Idle
                    _failures.tryEmit(SpeedTestFailure.INTERRUPTED)
                    return@launch
                }
                _state.value = SpeedTestState.Running((step + 1).toFloat() / PROGRESS_STEPS)
            }

            val result = measure()
            _state.value = SpeedTestState.Done(result)
            pingHistory.record(result)
        }
    }

    private fun measure(): SpeedTestResult {
        val random = Random(System.nanoTime())
        val vpnPenalty = if (networkMonitor.current().isVpnActive) VPN_PING_PENALTY_MS else 0

        val downloadMbps = round(random.nextDouble(MIN_DOWNLOAD_MBPS, MAX_DOWNLOAD_MBPS))
        val uploadMbps = round(downloadMbps * random.nextDouble(MIN_UPLOAD_SHARE, MAX_UPLOAD_SHARE))
        val pingMs = random.nextInt(MIN_PING_MS, MAX_PING_MS) + vpnPenalty
        val jitterMs = random.nextInt(MIN_JITTER_MS, MAX_JITTER_MS)
        val packetLossPercent = round(random.nextDouble(MAX_PACKET_LOSS_PERCENT))

        return SpeedTestResult(
            downloadMbps = downloadMbps,
            uploadMbps = uploadMbps,
            pingMs = pingMs,
            jitterMs = jitterMs,
            packetLossPercent = packetLossPercent,
            grade = grade(pingMs, jitterMs, packetLossPercent, downloadMbps),
        )
    }

    private fun grade(
        pingMs: Int,
        jitterMs: Int,
        packetLossPercent: Double,
        downloadMbps: Double,
    ): ConnectionGrade = ConnectionScoring.gradeOf(
        pingMs = pingMs,
        jitterMs = jitterMs,
        packetLossPercent = packetLossPercent,
        downloadMbps = downloadMbps,
    )

    private fun round(value: Double): Double = (value * DECIMALS).roundToInt() / DECIMALS

    private companion object {
        const val PROGRESS_STEPS = 40
        const val STEP_MILLIS = 160L
        const val DECIMALS = 10.0

        const val MIN_DOWNLOAD_MBPS = 4.0
        const val MAX_DOWNLOAD_MBPS = 120.0
        const val MIN_UPLOAD_SHARE = 0.2
        const val MAX_UPLOAD_SHARE = 0.6
        const val MIN_PING_MS = 12
        const val MAX_PING_MS = 70
        const val MIN_JITTER_MS = 1
        const val MAX_JITTER_MS = 28
        const val MAX_PACKET_LOSS_PERCENT = 3.0
        const val VPN_PING_PENALTY_MS = 24
    }
}
