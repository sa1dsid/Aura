package com.aura.feature.home.data.session

import com.aura.core.api.toTapRejection
import com.aura.core.common.ApplicationScope
import com.aura.core.common.parseIsoMillis
import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkType
import com.aura.core.system.EmulatorDetector
import com.aura.feature.home.data.local.TapSessionStore
import com.aura.feature.home.data.remote.HomeRemoteDataSource
import com.aura.feature.home.domain.model.COOLDOWN_DURATION
import com.aura.feature.home.domain.model.SPARK_RATE_MOBILE
import com.aura.feature.home.domain.model.SPARK_RATE_WIFI
import com.aura.feature.home.domain.model.SparkWindow
import com.aura.feature.home.domain.model.TEST_DURATION
import com.aura.feature.home.domain.model.TestSessionEvent
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.TestStartRejection
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val NETWORK_TYPE_WIFI = "wifi"

private const val NETWORK_TYPE_MOBILE = "mobile"

private const val STATUS_COMPLETED = "completed"

private const val REWARD_ION = 20

private const val EVENT_BUFFER = 8

private const val MILLIS_IN_SECOND = 1_000.0

private const val STATUS_RUNNING = "running"

@Singleton
class TestSessionEngine @Inject constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    private val remote: HomeRemoteDataSource,
    private val tapSessionStore: TapSessionStore,
    private val networkMonitor: NetworkMonitor,
    private val emulatorDetector: EmulatorDetector,
    private val pingHistory: PingHistoryRepository,
) {

    private val _state = MutableStateFlow<TestSessionState>(TestSessionState.Ready(REWARD_ION))
    val state: StateFlow<TestSessionState> = _state.asStateFlow()

    private val _spark = MutableStateFlow(SparkWindow())
    val spark: StateFlow<SparkWindow> = _spark.asStateFlow()

    private val _events = MutableSharedFlow<TestSessionEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<TestSessionEvent> = _events.asSharedFlow()

    private val mutex = Mutex()

    private var sessionId: String? = null
    private var runningEndsAt: Long? = null
    private var cooldownEndsAt: Long? = null
    private var pausedRemaining: Duration? = null
    private var isVpnPaused = false
    private var sparkSyncedAt = 0L
    private var isStarting = false
    private var isFinishing = false
    private var heartbeat: Job? = null

    init {
        scope.launch { releasePendingSession() }

        scope.launch {
            while (isActive) {
                delay(TICK)
                tick()
            }
        }
    }

    private suspend fun releasePendingSession(): Boolean {
        val pending = tapSessionStore.pendingSessionId() ?: return false

        val released = try {
            remote.finishTap(
                sessionId = pending,
                interrupted = true,
                networkLost = false,
                appBackgrounded = true,
            )
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: HttpException) {
            true
        } catch (error: Throwable) {
            false
        }

        if (released) tapSessionStore.clearPendingSessionId()
        return released
    }

    suspend fun syncFromDashboard(cooldownAvailableAt: String?, sparkBalance: String) {
        val rate = tapSessionStore.rate()

        mutex.withLock {
            if (runningEndsAt != null || isFinishing) return@withLock
            cooldownEndsAt = cooldownAvailableAt?.parseIsoMillis()
            if (isVpnPaused) pausedRemaining = cooldownEndsAt.remainingFromNow()
            applySpark(sparkBalance, rate)
        }

        tick()
    }

    fun start() {
        scope.launch {
            val allowed = mutex.withLock {
                if (isStarting || _state.value !is TestSessionState.Ready) {
                    false
                } else {
                    isStarting = true
                    true
                }
            }
            if (!allowed) return@launch

            val status = networkMonitor.current()
            val started = try {
                remote.startTap(
                    networkType = status.type.toRequestType(),
                    vpn = status.isVpnActive,
                    emulator = emulatorDetector.isEmulator,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                val retried = error.toTapRejection() == TestStartRejection.SessionStuck &&
                    releasePendingSession()

                mutex.withLock { isStarting = false }

                if (retried) start() else {
                    _events.tryEmit(TestSessionEvent.Rejected(error.toTapRejection()))
                }
                return@launch
            }

            val id = started.sessionId
            if (id == null) {
                mutex.withLock { isStarting = false }
                _events.tryEmit(TestSessionEvent.Rejected(TestStartRejection.Unavailable))
                return@launch
            }

            tapSessionStore.saveRate(started.sparkWindowRate.orDefaultFor(status.type))
            tapSessionStore.savePendingSessionId(id)

            mutex.withLock {
                isStarting = false
                sessionId = id
                runningEndsAt = System.currentTimeMillis() + TEST_DURATION.inWholeMilliseconds
            }

            startHeartbeat(id)
            tick()
        }
    }

    private fun startHeartbeat(sessionId: String) {
        heartbeat?.cancel()
        heartbeat = scope.launch {
            while (isActive) {
                val alive = try {
                    remote.tapHeartbeat(sessionId).status == STATUS_RUNNING
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    delay(HEARTBEAT_RETRY)
                    continue
                }

                if (!alive) {
                    dropSession(sessionId)
                    return@launch
                }

                delay(HEARTBEAT_INTERVAL)
            }
        }
    }

    private suspend fun dropSession(lostSessionId: String) {
        val dropped = mutex.withLock {
            if (sessionId != lostSessionId) return
            sessionId = null
            runningEndsAt = null
            true
        }

        if (dropped) tapSessionStore.clearPendingSessionId()

        _events.tryEmit(TestSessionEvent.Interrupted)
        tick()
    }

    fun interrupt(networkLost: Boolean = false) {
        scope.launch {
            val id = mutex.withLock {
                if (runningEndsAt == null) return@launch
                val current = sessionId ?: return@launch
                sessionId = null
                runningEndsAt = null
                current
            }

            heartbeat?.cancel()

            val released = runCatching {
                remote.finishTap(
                    sessionId = id,
                    interrupted = true,
                    networkLost = networkLost,
                    appBackgrounded = !networkLost,
                )
            }.isSuccess

            if (released) tapSessionStore.clearPendingSessionId()

            _events.tryEmit(TestSessionEvent.Interrupted)
            tick()
        }
    }

    fun onVpnChanged(active: Boolean) {
        scope.launch {
            val changed = mutex.withLock {
                if (isVpnPaused == active) return@launch
                isVpnPaused = active
                pausedRemaining = if (active) cooldownEndsAt.remainingFromNow() else null
                _spark.update { it.copy(isPaused = active) }
                true
            }
            if (!changed) return@launch

            val earning = try {
                remote.updateEarningState(vpn = active, emulator = emulatorDetector.isEmulator)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                null
            }

            var resumedCooldown = false

            if (earning != null) {
                mutex.withLock {
                    cooldownEndsAt = earning.cooldownAvailableAt?.parseIsoMillis()
                    pausedRemaining = if (active) cooldownEndsAt.remainingFromNow() else null
                    applySpark(earning.sparkBalance, _spark.value.ratePerWindow)
                    resumedCooldown = !active && cooldownEndsAt.remainingFromNow() > Duration.ZERO
                }
            }

            if (resumedCooldown) _events.tryEmit(TestSessionEvent.CooldownResumed)
            tick()
        }
    }

    private suspend fun tick() {
        val completedSession = mutex.withLock {
            val runningUntil = runningEndsAt ?: return@withLock advanceIdle()
            val left = (runningUntil - System.currentTimeMillis()).coerceAtLeast(0).milliseconds

            if (left > Duration.ZERO) {
                _state.value = TestSessionState.Running(
                    remaining = left,
                    total = TEST_DURATION,
                    rewardIon = REWARD_ION,
                )
                return@withLock null
            }

            runningEndsAt = null
            isFinishing = true
            heartbeat?.cancel()
            sessionId.also { sessionId = null }
        }

        if (completedSession != null) complete(completedSession)
    }

    private fun advanceIdle(): String? {
        if (isFinishing) return null

        val remaining = pausedRemaining ?: cooldownEndsAt.remainingFromNow()

        if (remaining > Duration.ZERO) {
            tickSpark()
            _state.value = TestSessionState.Cooldown(
                remaining = remaining,
                total = COOLDOWN_DURATION,
                isPausedByVpn = isVpnPaused,
            )
        } else {
            _spark.update { it.copy(isPaused = true) }
            _state.value = TestSessionState.Ready(REWARD_ION)
        }

        return null
    }

    private fun tickSpark() {
        if (isVpnPaused) {
            _spark.update { it.copy(isPaused = true) }
            return
        }

        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - sparkSyncedAt).coerceAtLeast(0) / MILLIS_IN_SECOND
        sparkSyncedAt = now

        _spark.update { window ->
            window.copy(
                balance = window.balance + window.perSecond * elapsedSeconds,
                isPaused = false,
            )
        }
    }

    private suspend fun complete(finishedSessionId: String) {
        val finished = try {
            remote.finishTap(
                sessionId = finishedSessionId,
                interrupted = false,
                networkLost = false,
                appBackgrounded = false,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            null
        }

        if (finished == null || finished.status != STATUS_COMPLETED) {
            mutex.withLock { isFinishing = false }
            _events.tryEmit(TestSessionEvent.Interrupted)
            tick()
            return
        }

        tapSessionStore.saveRate(finished.sparkWindowRate)

        mutex.withLock {
            isFinishing = false
            cooldownEndsAt = finished.cooldownAvailableAt?.parseIsoMillis()
                ?: System.currentTimeMillis() + COOLDOWN_DURATION.inWholeMilliseconds
            pausedRemaining = if (isVpnPaused) cooldownEndsAt.remainingFromNow() else null
            applySpark(finished.sparkBalance, finished.sparkWindowRate)
        }

        scope.launch { pingHistory.recordProbe(PingSource.HOME) }
        _events.tryEmit(TestSessionEvent.Completed(REWARD_ION))
        tick()
    }

    private fun applySpark(balance: String, rate: Int) {
        sparkSyncedAt = System.currentTimeMillis()
        _spark.value = SparkWindow(
            balance = balance.toDoubleOrNull() ?: _spark.value.balance,
            ratePerWindow = rate,
            isPaused = isVpnPaused,
        )
    }

    private fun Long?.remainingFromNow(): Duration =
        this?.minus(System.currentTimeMillis())?.coerceAtLeast(0)?.milliseconds ?: Duration.ZERO

    private fun Int.orDefaultFor(type: NetworkType): Int =
        if (this > 0) this else if (type == NetworkType.WIFI) SPARK_RATE_WIFI else SPARK_RATE_MOBILE

    private fun NetworkType.toRequestType(): String =
        if (this == NetworkType.WIFI) NETWORK_TYPE_WIFI else NETWORK_TYPE_MOBILE

    private companion object {
        val TICK = 1.seconds
        val HEARTBEAT_INTERVAL = 10.seconds
        val HEARTBEAT_RETRY = 1.seconds
    }
}
