package com.aura.feature.home.data.session

import com.aura.core.common.ApplicationScope
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Singleton
class TestSessionEngine @Inject constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    private val pingHistory: PingHistoryRepository,
) {

    private val _state = MutableStateFlow<TestSessionState>(INITIAL_STATE)
    val state: StateFlow<TestSessionState> = _state.asStateFlow()

    init {
        scope.launch {
            _state.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { hasSubscribers ->
                    if (!hasSubscribers) return@collectLatest
                    while (isActive) {
                        delay(1.seconds)
                        val before = _state.value
                        _state.update { it.tickOneSecond() }
                        if (before is TestSessionState.Running &&
                            _state.value is TestSessionState.Cooldown
                        ) {
                            scope.launch { pingHistory.recordProbe() }
                        }
                    }
                }
        }
    }

    fun startTest(rewardIon: Int) {
        if (_state.value !is TestSessionState.Ready) return

        _state.value = TestSessionState.Running(
            remaining = TEST_DURATION,
            total = TEST_DURATION,
            rewardIon = rewardIon,
        )
    }

    fun interrupt(rewardIon: Int) {
        if (_state.value is TestSessionState.Running) {
            _state.value = TestSessionState.Ready(rewardIon)
        }
    }

    fun onVpnChanged(active: Boolean) {
        _state.update { current ->
            if (current is TestSessionState.Cooldown) current.copy(isPausedByVpn = active)
            else current
        }
    }

    private fun TestSessionState.tickOneSecond(): TestSessionState = when (this) {
        is TestSessionState.Ready -> this

        is TestSessionState.Running -> {
            val left = remaining - 1.seconds
            if (left <= Duration.ZERO) {
                TestSessionState.Cooldown(
                    remaining = COOLDOWN,
                    total = COOLDOWN,
                    isPausedByVpn = false,
                )
            } else {
                copy(remaining = left)
            }
        }

        is TestSessionState.Cooldown -> when {
            isPausedByVpn -> this
            remaining - 1.seconds <= Duration.ZERO -> TestSessionState.Ready(DEFAULT_REWARD_ION)
            else -> copy(remaining = remaining - 1.seconds)
        }
    }

    private companion object {
        val TEST_DURATION = 3.minutes
        val COOLDOWN = 12.hours
        const val DEFAULT_REWARD_ION = 20

        val INITIAL_STATE = TestSessionState.Ready(DEFAULT_REWARD_ION)
    }
}
