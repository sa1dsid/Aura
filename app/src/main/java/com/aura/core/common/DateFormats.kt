package com.aura.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

private const val ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

private const val ISO_SECONDS_LENGTH = 19

private const val MILLIS_DIGITS = 3

private val isoFormat = ThreadLocal.withInitial {
    SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

fun String.parseIsoMillis(): Long? {
    val text = trim()
    if (text.length < ISO_SECONDS_LENGTH) return null

    val seconds = runCatching {
        isoFormat.get()!!.parse(text.substring(0, ISO_SECONDS_LENGTH))
    }.getOrNull()?.time ?: return null

    val tail = text.substring(ISO_SECONDS_LENGTH)
    return seconds + tail.fractionMillis() + tail.offsetMillis()
}

private fun String.fractionMillis(): Long {
    if (!startsWith('.')) return 0
    val digits = drop(1).takeWhile(Char::isDigit)
    if (digits.isEmpty()) return 0
    return digits.take(MILLIS_DIGITS).padEnd(MILLIS_DIGITS, '0').toLong()
}

private fun String.offsetMillis(): Long {
    val sign = when {
        contains('+') -> -1
        lastIndexOf('-') > 0 -> 1
        else -> return 0
    }
    val offset = substring(indexOfLast { it == '+' || it == '-' } + 1)
    val hours = offset.substringBefore(':').toLongOrNull() ?: return 0
    val minutes = offset.substringAfter(':', "0").toLongOrNull() ?: 0
    return sign * (hours * MINUTES_IN_HOUR + minutes) * SECONDS_IN_MINUTE * MILLIS_IN_SECOND
}

private const val MINUTES_IN_HOUR = 60L

private const val SECONDS_IN_MINUTE = 60L

private const val MILLIS_IN_SECOND = 1_000L
