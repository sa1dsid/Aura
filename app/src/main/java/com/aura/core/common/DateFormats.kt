package com.aura.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val CLOCK_PATTERN = "HH:mm"

private const val DAY_PATTERN = "MMM dd"

private const val DAYS_IN_YEAR = 1000

private val clockFormat = ThreadLocal.withInitial {
    SimpleDateFormat(CLOCK_PATTERN, Locale.US)
}

private val dayFormat = ThreadLocal.withInitial {
    SimpleDateFormat(DAY_PATTERN, Locale.US)
}

private val dayKeyCalendar = ThreadLocal.withInitial { Calendar.getInstance() }

fun Long.formatClock(): String = format(clockFormat)

fun Long.formatDayShort(): String = format(dayFormat)

fun Long.dayKey(): Int {
    val calendar = dayKeyCalendar.get()!!
    calendar.timeInMillis = this
    return calendar.get(Calendar.YEAR) * DAYS_IN_YEAR + calendar.get(Calendar.DAY_OF_YEAR)
}

private fun Long.format(format: ThreadLocal<SimpleDateFormat>): String =
    format.get()!!.format(Date(this))
