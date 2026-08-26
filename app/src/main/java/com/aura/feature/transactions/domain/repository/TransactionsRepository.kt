package com.aura.feature.transactions.domain.repository

import com.aura.feature.transactions.domain.model.TransactionEvent
import kotlinx.coroutines.flow.Flow

interface TransactionsRepository {

    val transactions: Flow<List<TransactionEvent>>

    suspend fun load(): Boolean
}
