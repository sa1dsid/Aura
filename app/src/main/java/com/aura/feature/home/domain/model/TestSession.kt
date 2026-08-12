package com.aura.feature.home.domain.model

import kotlin.time.Duration

sealed interface TestSessionState {

    data class Ready(val rewardIon: Int) : TestSessionState

    data class Running(
        val remaining: Duration,
        val total: Duration,
        val rewardIon: Int,
    ) : TestSessionState {
        val progress: Float
            get() = if (total.inWholeMilliseconds == 0L) 0f
            else 1f - (remaining.inWholeMilliseconds.toFloat() / total.inWholeMilliseconds)
    }

    data class Cooldown(
        val remaining: Duration,
        val total: Duration,
        val isPausedByVpn: Boolean,
    ) : TestSessionState
}

sealed interface TestSessionEvent {
    data class Completed(val rewardIon: Int) : TestSessionEvent
    data object Interrupted : TestSessionEvent
    data object CooldownResumed : TestSessionEvent
}

sealed interface TestStartRejection {
    data object DataShareDisabled : TestStartRejection
    data object VpnDetected : TestStartRejection
    data class CooldownNotFinished(val remaining: Duration) : TestStartRejection
}

fun testStartRejection(
    session: TestSessionState,
    isVpnActive: Boolean,
): TestStartRejection? = when {
    isVpnActive -> TestStartRejection.VpnDetected
    session is TestSessionState.Cooldown -> TestStartRejection.CooldownNotFinished(session.remaining)
    else -> null
}
