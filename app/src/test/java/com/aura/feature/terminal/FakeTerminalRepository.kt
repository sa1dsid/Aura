package com.aura.feature.terminal

import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.terminal.domain.model.TerminalCounters
import com.aura.feature.terminal.domain.repository.TerminalRepository
import com.aura.feature.transactions.domain.model.TransactionEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FakeTerminalRepository(
    private val storedEvents: List<TransactionEvent> = emptyList(),
    private val storedCodes: List<PromoCode> = emptyList(),
) : TerminalRepository {

    private val loadedCounters = MutableStateFlow(TerminalCounters())

    private val loadedTransactions = MutableStateFlow(emptyList<TransactionEvent>())

    private val loadedPromoCodes = MutableStateFlow(emptyList<PromoCode>())

    var failNextLoad = false

    var gate: CompletableDeferred<Unit>? = null

    var transactionsLoadCount = 0
        private set

    var promoCodesLoadCount = 0
        private set

    var refreshCount = 0
        private set

    override val counters: Flow<TerminalCounters> = loadedCounters

    override val transactions: Flow<List<TransactionEvent>> = loadedTransactions

    override val promoCodes: Flow<List<PromoCode>> = loadedPromoCodes

    override suspend fun refreshCounters() {
        refreshCount++
    }

    fun emit(counters: TerminalCounters) {
        loadedCounters.value = counters
    }

    override fun clearTransactionsCounter() {
        loadedCounters.update { it.copy(unreadTransactions = 0) }
    }

    override fun clearPromoCodesCounter() {
        loadedCounters.update { it.copy(unreadPromoCodes = 0) }
    }

    override suspend fun openTransactions(): Boolean {
        transactionsLoadCount++
        gate?.await()
        if (failNextLoad) {
            failNextLoad = false
            return false
        }

        loadedTransactions.value = storedEvents
        clearTransactionsCounter()
        return true
    }

    override suspend fun openPromoCodes(): Boolean {
        promoCodesLoadCount++
        gate?.await()
        if (failNextLoad) {
            failNextLoad = false
            return false
        }

        loadedPromoCodes.value = storedCodes
        clearPromoCodesCounter()
        return true
    }
}
