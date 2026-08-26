package com.aura.feature.transactions

import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.repository.TransactionsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeTransactionsRepository(
    private val stored: List<TransactionEvent> = emptyList(),
) : TransactionsRepository {

    private val loaded = MutableStateFlow(emptyList<TransactionEvent>())

    var failNextLoad = false

    var gate: CompletableDeferred<Unit>? = null

    var loadCount = 0
        private set

    override val transactions: Flow<List<TransactionEvent>> = loaded

    override suspend fun load(): Boolean {
        loadCount++
        gate?.await()
        if (failNextLoad) {
            failNextLoad = false
            return false
        }

        loaded.value = stored
        return true
    }
}
