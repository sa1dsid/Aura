package com.aura.feature.home.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

val IONI_CHARGE_WINDOW = 24.hours

enum class IoniState { COMING, GEO_BLOCKED, ACTIVE }

data class IoniCard(
    val state: IoniState = IoniState.COMING,
    val lastCompletedTapAt: Long? = null,
)

fun IoniCard.chargeLeft(nowMillis: Long): Duration {
    if (state != IoniState.ACTIVE) return Duration.ZERO
    val tap = lastCompletedTapAt ?: return Duration.ZERO
    val elapsed = (nowMillis - tap).milliseconds
    if (elapsed <= Duration.ZERO) return IONI_CHARGE_WINDOW
    return (IONI_CHARGE_WINDOW - elapsed).coerceAtLeast(Duration.ZERO)
}

fun Duration.chargeFraction(): Float =
    (this / IONI_CHARGE_WINDOW).toFloat().coerceIn(0f, 1f)

fun Duration.chargeHours(): Int {
    val minutes = inWholeMinutes
    if (minutes <= 0L) return 0
    return ((minutes + MINUTES_PER_HOUR - 1) / MINUTES_PER_HOUR).toInt()
}

private const val MINUTES_PER_HOUR = 60L
