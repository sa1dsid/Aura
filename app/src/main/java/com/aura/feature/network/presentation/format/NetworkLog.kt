package com.aura.feature.network.presentation.format

import com.aura.feature.network.domain.model.PingRecord

sealed interface NetworkLogLine {

    data class DayComment(val timestamp: Long, val count: Int) : NetworkLogLine

    data class Entry(val record: PingRecord) : NetworkLogLine
}

fun List<PingRecord>.toLogLines(): List<NetworkLogLine> {
    if (isEmpty()) return emptyList()

    val keys = IntArray(size) { this[it].timestamp.dayKey() }
    val lines = ArrayList<NetworkLogLine>(size + 1)
    var currentKey: Int? = null

    forEachIndexed { index, record ->
        val key = keys[index]
        if (key != currentKey) {
            currentKey = key
            var runEnd = index
            while (runEnd < size && keys[runEnd] == key) runEnd++
            lines += NetworkLogLine.DayComment(
                timestamp = record.timestamp,
                count = runEnd - index,
            )
        }
        lines += NetworkLogLine.Entry(record)
    }

    return lines
}
