package com.aura.feature.network.presentation.format

import com.aura.feature.network.domain.model.PingRecord

sealed interface NetworkLogLine {

    data class DayComment(val timestamp: Long, val count: Int) : NetworkLogLine

    data class Entry(val record: PingRecord) : NetworkLogLine
}

fun List<PingRecord>.toLogLines(): List<NetworkLogLine> {
    if (isEmpty()) return emptyList()

    val lines = mutableListOf<NetworkLogLine>()
    var currentKey: Int? = null

    forEachIndexed { index, record ->
        val key = record.timestamp.dayKey()
        if (key != currentKey) {
            currentKey = key
            lines += NetworkLogLine.DayComment(
                timestamp = record.timestamp,
                count = drop(index).takeWhile { it.timestamp.dayKey() == key }.size,
            )
        }
        lines += NetworkLogLine.Entry(record)
    }

    return lines
}
