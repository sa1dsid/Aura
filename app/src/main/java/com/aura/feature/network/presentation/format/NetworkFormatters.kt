package com.aura.feature.network.presentation.format

import com.aura.feature.network.domain.model.ConnectionGrade
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val CLOCK_PATTERN = "HH:mm"

private const val LOG_DAY_PATTERN = "MMM d"

private const val BADGE_DAY_PATTERN = "MMM d"

fun Long.formatClockTime(): String = format(CLOCK_PATTERN)

fun Long.formatLogDay(): String = format(LOG_DAY_PATTERN)

fun Long.formatBadgeDay(): String = format(BADGE_DAY_PATTERN).uppercase(Locale.US)

fun Long.isSameDayAs(other: Long): Boolean {
    val first = calendarOf(this)
    val second = calendarOf(other)
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

fun Long.dayKey(): Int {
    val calendar = calendarOf(this)
    return calendar.get(Calendar.YEAR) * DAYS_IN_YEAR + calendar.get(Calendar.DAY_OF_YEAR)
}

fun Double.formatSpeed(): String = String.format(Locale.US, "%.1f", this)

fun Double.formatPacketLoss(): String = String.format(Locale.US, "%.1f", this)

fun ConnectionGrade.formatScore(): String = String.format(Locale.US, "%.1f", score)

private fun Long.format(pattern: String): String =
    SimpleDateFormat(pattern, Locale.US).format(Date(this))

private fun calendarOf(millis: Long): Calendar =
    Calendar.getInstance().apply { timeInMillis = millis }

private const val DAYS_IN_YEAR = 1000
