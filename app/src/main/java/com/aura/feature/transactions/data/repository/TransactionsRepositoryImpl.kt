package com.aura.feature.transactions.data.repository

import com.aura.core.api.dto.TransactionDto
import com.aura.core.common.IoDispatcher
import com.aura.core.common.runCatchingCancellable
import com.aura.core.session.SessionCache
import com.aura.feature.transactions.data.mapper.toDomain
import com.aura.feature.transactions.data.remote.TransactionsRemoteDataSource
import com.aura.feature.transactions.domain.model.TRANSACTIONS_LIMIT
import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.repository.TransactionsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionsRepositoryImpl @Inject constructor(
    private val remote: TransactionsRemoteDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransactionsRepository, SessionCache {

    private val loaded = MutableStateFlow(emptyList<TransactionEvent>())

    override val transactions: Flow<List<TransactionEvent>> = loaded.asStateFlow()

    override suspend fun clearSession() {
        loaded.value = emptyList()
    }

    override suspend fun load(): Boolean = withContext(ioDispatcher) {
        val events = runCatchingCancellable { remote.transactions() }.getOrNull()
            ?: return@withContext false

        loaded.value = events
            .mapNotNull(TransactionDto::toDomain)
            .sortedByDescending(TransactionEvent::timestamp)
            .take(TRANSACTIONS_LIMIT)
        true
    }
}
