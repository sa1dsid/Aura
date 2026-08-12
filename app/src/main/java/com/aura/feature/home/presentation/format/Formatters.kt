package com.aura.feature.home.presentation.format

import java.util.Locale
import kotlin.time.Duration

fun Long.formatGrouped(): String = String.format(Locale.US, "%,d", this)

fun Int.formatGrouped(): String = String.format(Locale.US, "%,d", this)

fun Double.formatRate(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

fun Duration.formatMinutesSeconds(): String = toComponents { minutes, seconds, _ ->
    String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun Duration.formatHoursMinutesSeconds(): String = toComponents { hours, minutes, seconds, _ ->
    String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun Duration.formatHoursMinutes(): String = toComponents { hours, minutes, _, _ ->
    String.format(Locale.US, "%02d:%02d", hours, minutes)
}
