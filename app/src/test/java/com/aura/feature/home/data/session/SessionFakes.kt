package com.aura.feature.home.data.session

import com.aura.core.api.dto.BatteryOptimizationDto
import com.aura.core.api.dto.DashboardDto
import com.aura.core.api.dto.EarningStateDto
import com.aura.core.api.dto.HeartbeatDto
import com.aura.core.api.dto.MeshDto
import com.aura.core.api.dto.TapStateDto
import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkStatus
import com.aura.core.network.NetworkType
import com.aura.feature.home.data.local.TapSessionStore
import com.aura.feature.home.data.remote.HomeRemoteDataSource
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal const val SPARK_RATE_ON_WIFI = 20_000

internal fun isoAt(millis: Long): String = SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    Locale.US,
).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))

internal val STALLED_BEAT = 1.minutes

internal class FakeHomeRemoteDataSource(
    private val scheduler: TestCoroutineScheduler,
    private val startDelay: Duration = Duration.ZERO,
    private val finishDelay: Duration = Duration.ZERO,
) : HomeRemoteDataSource {

    var heartbeats = 0
        private set

    var heartbeatStatus = "running"

    var stalledBeats = 0

    var cooldownShift = 0L

    var startRate = SPARK_RATE_ON_WIFI

    val interruptedSessions = mutableListOf<String>()

    val startedSessions = mutableListOf<String>()

    private var sessions = 0
    private var cooldownEndsAt: Long? = null

    override suspend fun dashboard(): DashboardDto = DashboardDto(
        cooldownAvailableAt = cooldownEndsAt?.let(::isoAt),
    )

    override suspend fun startTap(
        networkType: String,
        vpn: Boolean,
        emulator: Boolean,
    ): TapStateDto {
        if (startDelay > Duration.ZERO) delay(startDelay)
        sessions++
        startedSessions += "session-$sessions"

        return TapStateDto(
            sessionId = "session-$sessions",
            status = "running",
            sparkWindowRate = startRate,
        )
    }

    override suspend fun tapHeartbeat(sessionId: String): TapStateDto {
        if (stalledBeats > 0) {
            stalledBeats--
            delay(STALLED_BEAT)
        }

        heartbeats++
        return TapStateDto(sessionId = sessionId, status = heartbeatStatus)
    }

    override suspend fun finishTap(
        sessionId: String,
        interrupted: Boolean,
        networkLost: Boolean,
        appBackgrounded: Boolean,
    ): TapStateDto {
        if (!interrupted && finishDelay > Duration.ZERO) delay(finishDelay)

        if (interrupted) {
            interruptedSessions += sessionId
            return TapStateDto(sessionId = sessionId, status = "interrupted")
        }

        cooldownEndsAt = scheduler.currentTime + 12.hours.inWholeMilliseconds

        return TapStateDto(
            sessionId = sessionId,
            status = "completed",
            tapCount = 1,
            accruedIon = 20,
            cooldownAvailableAt = isoAt(cooldownEndsAt!!),
            sparkWindowRate = SPARK_RATE_ON_WIFI,
        )
    }

    override suspend fun updateEarningState(vpn: Boolean?, emulator: Boolean?): EarningStateDto {
        if (vpn == false) cooldownEndsAt = cooldownEndsAt?.plus(cooldownShift)

        return EarningStateDto(
            paused = vpn == true,
            cooldownAvailableAt = cooldownEndsAt?.let(::isoAt),
        )
    }

    override suspend fun batteryOptimization(): BatteryOptimizationDto = BatteryOptimizationDto()

    override suspend fun declineBatteryOptimization(): BatteryOptimizationDto =
        BatteryOptimizationDto()

    override suspend fun confirmBatteryOptimizationDisabled(): BatteryOptimizationDto =
        BatteryOptimizationDto()

    override suspend fun heartbeat(): HeartbeatDto = HeartbeatDto()

    override suspend fun updateLocation(vpn: Boolean): String? = null

    override suspend fun mesh(): MeshDto = MeshDto()

    override suspend fun markBonusTeaserSeen() = Unit

    override suspend fun markBonusCongratulationSeen() = Unit
    override suspend fun markSparkCouponSeen() = Unit
}

internal class FakeTapSessionStore : TapSessionStore {

    private var rate = 0
    private var pending: String? = null

    override suspend fun rate(): Int = rate

    override suspend fun saveRate(rate: Int) {
        this.rate = rate
    }

    override suspend fun pendingSessionId(): String? = pending

    override suspend fun savePendingSessionId(sessionId: String) {
        pending = sessionId
    }

    override suspend fun clearPendingSessionId() {
        pending = null
    }
}

internal class FakeNetworkMonitor : NetworkMonitor {

    private val state = MutableStateFlow(
        NetworkStatus(isOnline = true, isVpnActive = false, type = NetworkType.WIFI)
    )

    override val status = state

    override fun current(): NetworkStatus = state.value
}

internal class FakePingHistoryRepository : PingHistoryRepository {

    override fun observeHistory(): Flow<List<PingRecord>> = flowOf(emptyList())

    override suspend fun refresh() = Unit

    override suspend fun recordProbe(source: PingSource) = Unit

    override suspend fun record(result: SpeedTestResult, source: PingSource) = Unit
}

internal val HEARTBEAT_WINDOW = 5.seconds
