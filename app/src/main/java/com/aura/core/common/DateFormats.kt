package com.aura.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val CLOCK_PATTERN = "HH:mm"

private const val DAY_PATTERN = "MMM dd"

private const val DAYS_IN_YEAR = 1000

fun Long.formatClock(): String = format(CLOCK_PATTERN)

fun Long.formatDayShort(): String = format(DAY_PATTERN)

fun Long.dayKey(): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@dayKey }
    return calendar.get(Calendar.YEAR) * DAYS_IN_YEAR + calendar.get(Calendar.DAY_OF_YEAR)
}

private fun Long.format(pattern: String): String =
    SimpleDateFormat(pattern, Locale.US).format(Date(this))
