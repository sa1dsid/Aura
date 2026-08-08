package com.aura.feature.home.presentation.format

import com.aura.feature.home.domain.model.NodeTier
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

fun NodeTier.displayName(): String = when (this) {
    NodeTier.IDLE_NODE -> "Idle Node"
    NodeTier.ACTIVE_SIGNAL -> "Active Signal"
    NodeTier.STABLE_LINK -> "Stable Link"
    NodeTier.CORE_NODE -> "Core Node"
    NodeTier.IONIC_PRIME -> "IONIC Prime"
}

fun NodeTier.stepLabel(): String = when (this) {
    NodeTier.IDLE_NODE -> "Idle\nNode"
    NodeTier.ACTIVE_SIGNAL -> "Active\nSignal"
    NodeTier.STABLE_LINK -> "Stable\nLink"
    NodeTier.CORE_NODE -> "Core\nNode"
    NodeTier.IONIC_PRIME -> "IONIC\nPrime"
}
