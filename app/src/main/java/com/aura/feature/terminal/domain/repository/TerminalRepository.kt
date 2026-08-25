package com.aura.feature.terminal.domain.repository

import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.terminal.domain.model.TerminalCounters
import com.aura.feature.transactions.domain.model.TransactionEvent
import kotlinx.coroutines.flow.Flow

interface TerminalRepository {
    val counters: Flow<TerminalCounters>

    val transactions: Flow<List<TransactionEvent>>

    val promoCodes: Flow<List<PromoCode>>

    suspend fun refreshCounters()

    fun clearTransactionsCounter()

    fun clearPromoCodesCounter()

    suspend fun openTransactions(): Boolean

    suspend fun openPromoCodes(): Boolean
}
