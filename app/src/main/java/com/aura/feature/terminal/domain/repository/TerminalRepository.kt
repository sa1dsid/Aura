package com.aura.feature.terminal.domain.repository

import com.aura.feature.terminal.domain.model.TerminalCounters
import kotlinx.coroutines.flow.Flow

interface TerminalRepository {

    val counters: Flow<TerminalCounters>

    suspend fun refreshCounters()

    fun clearTransactionsCounter()

    fun clearPromoCodesCounter()
}
