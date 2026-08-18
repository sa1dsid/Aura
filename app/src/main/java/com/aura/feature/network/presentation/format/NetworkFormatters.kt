package com.aura.feature.network.presentation.format

import com.aura.feature.network.domain.model.ConnectionGrade
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val CLOCK_PATTERN = "HH:mm"

private const val LOG_DAY_PATTERN = "MMM d"

private const val BADGE_DAY_PATTERN = "MMM d"

private val clockFormat = ThreadLocal.withInitial {
    SimpleDateFormat(CLOCK_PATTERN, Locale.US)
}

private val logDayFormat = ThreadLocal.withInitial {
    SimpleDateFormat(LOG_DAY_PATTERN, Locale.US)
}

private val badgeDayFormat = ThreadLocal.withInitial {
    SimpleDateFormat(BADGE_DAY_PATTERN, Locale.US)
}

private val sharedCalendar = ThreadLocal.withInitial { Calendar.getInstance() }

fun Long.formatClockTime(): String = format(clockFormat)

fun Long.formatLogDay(): String = format(logDayFormat)

fun Long.formatBadgeDay(): String = format(badgeDayFormat).uppercase(Locale.US)

fun Long.isSameDayAs(other: Long): Boolean = dayKey() == other.dayKey()

fun Long.dayKey(): Int {
    val calendar = sharedCalendar.get()!!
    calendar.timeInMillis = this
    return calendar.get(Calendar.YEAR) * DAYS_IN_YEAR + calendar.get(Calendar.DAY_OF_YEAR)
}

fun Double.formatSpeed(): String = String.format(Locale.US, "%.1f", this)

fun Double.formatPacketLoss(): String = String.format(Locale.US, "%.1f", this)

fun ConnectionGrade.formatScore(): String = String.format(Locale.US, "%.1f", score)

private fun Long.format(format: ThreadLocal<SimpleDateFormat>): String =
    format.get()!!.format(Date(this))

private const val DAYS_IN_YEAR = 1000
