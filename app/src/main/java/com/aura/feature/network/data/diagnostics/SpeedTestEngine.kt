package com.aura.feature.network.data.diagnostics

import com.aura.core.common.ApplicationScope
import com.aura.core.network.NetworkMonitor
import com.aura.core.session.SessionCache
import com.aura.feature.network.domain.model.ConnectionGrade
import com.aura.feature.network.domain.model.ConnectionScoring
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestFailure
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.model.SpeedTestState
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

private const val PING_ATTEMPTS = 12

private const val PING_PHASE_SHARE = 0.15f

private const val DOWNLOAD_PHASE_SHARE = 0.6f

private const val UPLOAD_PHASE_SHARE = 0.25f

private const val DECIMALS = 10.0

@Singleton
class SpeedTestEngine @Inject constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    private val networkMonitor: NetworkMonitor,
    private val pingProbe: PingProbe,
    private val throughputProbe: ThroughputProbe,
    private val pingHistory: PingHistoryRepository,
) : SessionCache {

    private val _state = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    private val _failures = MutableSharedFlow<SpeedTestFailure>(extraBufferCapacity = 1)
    val failures: SharedFlow<SpeedTestFailure> = _failures.asSharedFlow()

    private var running: Job? = null

    fun start() {
        if (_state.value is SpeedTestState.Running) return

        if (!networkMonitor.current().isOnline) {
            _state.value = SpeedTestState.Idle
            _failures.tryEmit(SpeedTestFailure.NO_CONNECTION)
            return
        }

        running = scope.launch {
            _state.value = SpeedTestState.Running(0f)

            val sample = pingProbe.sample(PING_ATTEMPTS)
            val pingMs = sample.pingMs
            if (pingMs == null) {
                fail(SpeedTestFailure.INTERRUPTED)
                return@launch
            }
            _state.value = SpeedTestState.Running(PING_PHASE_SHARE)

            val download = throughputProbe.download { progress ->
                _state.value = SpeedTestState.Running(
                    PING_PHASE_SHARE + DOWNLOAD_PHASE_SHARE * progress
                )
            }
            if (download == null) {
                fail(SpeedTestFailure.INTERRUPTED)
                return@launch
            }

            val upload = throughputProbe.upload { progress ->
                _state.value = SpeedTestState.Running(
                    PING_PHASE_SHARE + DOWNLOAD_PHASE_SHARE + UPLOAD_PHASE_SHARE * progress
                )
            }
            if (upload == null) {
                fail(SpeedTestFailure.INTERRUPTED)
                return@launch
            }

            val downloadMbps = download.megabitsPerSecond.round()
            val jitterMs = sample.jitterMs ?: 0
            val packetLossPercent = sample.packetLossPercent.round()

            val result = SpeedTestResult(
                downloadMbps = downloadMbps,
                uploadMbps = upload.megabitsPerSecond.round(),
                pingMs = pingMs,
                jitterMs = jitterMs,
                packetLossPercent = packetLossPercent,
                grade = ConnectionScoring.gradeOf(
                    pingMs = pingMs,
                    jitterMs = jitterMs,
                    packetLossPercent = packetLossPercent,
                    downloadMbps = downloadMbps,
                ),
            )

            _state.value = SpeedTestState.Done(result)
            pingHistory.record(result, PingSource.DIAGNOSTIC)
        }
    }

    override suspend fun clearSession() = cancel()

    fun cancel() {
        running?.cancel()
        running = null
        _state.value = SpeedTestState.Idle
    }

    private fun fail(failure: SpeedTestFailure) {
        _state.value = SpeedTestState.Idle
        _failures.tryEmit(
            if (networkMonitor.current().isOnline) failure else SpeedTestFailure.NO_CONNECTION
        )
    }

    private fun Double.round(): Double = (this * DECIMALS).roundToInt() / DECIMALS
}
