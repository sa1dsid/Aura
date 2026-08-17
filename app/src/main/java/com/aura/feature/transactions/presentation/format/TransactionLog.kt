package com.aura.feature.transactions.presentation.format

import com.aura.core.common.dayKey
import com.aura.feature.transactions.domain.model.TransactionEvent

sealed interface TransactionLogLine {

    data class DayComment(val timestamp: Long, val count: Int) : TransactionLogLine

    data class Entry(val event: TransactionEvent) : TransactionLogLine
}

fun List<TransactionEvent>.toLogLines(): List<TransactionLogLine> {
    if (isEmpty()) return emptyList()

    val keys = IntArray(size) { this[it].timestamp.dayKey() }
    val lines = ArrayList<TransactionLogLine>(size + 1)
    var currentKey: Int? = null

    forEachIndexed { index, event ->
        val key = keys[index]
        if (key != currentKey) {
            currentKey = key
            var runEnd = index
            while (runEnd < size && keys[runEnd] == key) runEnd++
            lines += TransactionLogLine.DayComment(
                timestamp = event.timestamp,
                count = runEnd - index,
            )
        }
        lines += TransactionLogLine.Entry(event)
    }

    return lines
}
