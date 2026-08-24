package com.aura.feature.home.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

val TEST_DURATION: Duration = 3.minutes

val COOLDOWN_DURATION: Duration = 12.hours

const val SPARK_WINDOW_SECONDS = 43_200.0

const val SPARK_RATE_WIFI = 20_000

const val SPARK_RATE_MOBILE = 40_000

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

data class SparkWindow(
    val balance: Double = 0.0,
    val ratePerWindow: Int = 0,
    val isPaused: Boolean = false,
) {
    val perSecond: Double get() = ratePerWindow / SPARK_WINDOW_SECONDS
}

sealed interface TestSessionEvent {
    data class Completed(val rewardIon: Int) : TestSessionEvent
    data object Interrupted : TestSessionEvent
    data object CooldownResumed : TestSessionEvent
    data class Rejected(val rejection: TestStartRejection) : TestSessionEvent
}

sealed interface TestStartRejection {
    data object DataShareDisabled : TestStartRejection
    data object VpnDetected : TestStartRejection
    data object UnsupportedDevice : TestStartRejection
    data object Unavailable : TestStartRejection
    data class CooldownNotFinished(val remaining: Duration) : TestStartRejection
}

fun testStartRejection(
    session: TestSessionState,
    isVpnActive: Boolean,
    isDataShareRequired: Boolean = false,
    isDataShareEnabled: Boolean = true,
): TestStartRejection? = when {
    isVpnActive -> TestStartRejection.VpnDetected
    isDataShareRequired && !isDataShareEnabled -> TestStartRejection.DataShareDisabled
    session is TestSessionState.Cooldown -> TestStartRejection.CooldownNotFinished(session.remaining)
    else -> null
}
