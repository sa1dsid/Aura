package com.aura.feature.terminal

import com.aura.feature.terminal.domain.model.TerminalCounters
import com.aura.feature.terminal.domain.repository.TerminalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FakeTerminalRepository : TerminalRepository {

    private val unread = MutableStateFlow(TerminalCounters())

    var refreshCount = 0
        private set

    override val counters: Flow<TerminalCounters> = unread

    override suspend fun refreshCounters() {
        refreshCount++
    }

    override fun clearTransactionsCounter() {
        unread.update { it.copy(unreadTransactions = 0) }
    }

    override fun clearPromoCodesCounter() {
        unread.update { it.copy(unreadPromoCodes = 0) }
    }

    fun emit(counters: TerminalCounters) {
        unread.value = counters
    }
}
