package com.aura.feature.transactions.presentation.format

import com.aura.core.common.dayKey
import com.aura.feature.transactions.domain.model.TransactionEvent

sealed interface TransactionLogLine {

    data class DayComment(val timestamp: Long, val count: Int) : TransactionLogLine

    data class Entry(val event: TransactionEvent) : TransactionLogLine
}

fun List<TransactionEvent>.toLogLines(): List<TransactionLogLine> {
    if (isEmpty()) return emptyList()

    val lines = mutableListOf<TransactionLogLine>()
    var currentKey: Int? = null

    forEachIndexed { index, event ->
        val key = event.timestamp.dayKey()
        if (key != currentKey) {
            currentKey = key
            lines += TransactionLogLine.DayComment(
                timestamp = event.timestamp,
                count = drop(index).takeWhile { it.timestamp.dayKey() == key }.size,
            )
        }
        lines += TransactionLogLine.Entry(event)
    }

    return lines
}
