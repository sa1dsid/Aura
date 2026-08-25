package com.aura.feature.terminal.data.repository

import com.aura.core.api.dto.PromoDto
import com.aura.core.api.dto.TransactionDto
import com.aura.core.common.IoDispatcher
import com.aura.core.session.SessionCache
import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.terminal.data.mapper.toDomain
import com.aura.feature.terminal.data.remote.TerminalRemoteDataSource
import com.aura.feature.terminal.domain.model.TerminalCounters
import com.aura.feature.terminal.domain.repository.TerminalRepository
import com.aura.feature.transactions.domain.model.TRANSACTIONS_LIMIT
import com.aura.feature.transactions.domain.model.TransactionEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalRepositoryImpl @Inject constructor(
    private val remote: TerminalRemoteDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TerminalRepository, SessionCache {

    private val _counters = MutableStateFlow(TerminalCounters())
    private val _transactions = MutableStateFlow(emptyList<TransactionEvent>())
    private val _promoCodes = MutableStateFlow(emptyList<PromoCode>())

    override val counters: Flow<TerminalCounters> = _counters.asStateFlow()

    override val transactions: Flow<List<TransactionEvent>> = _transactions.asStateFlow()

    override val promoCodes: Flow<List<PromoCode>> = _promoCodes.asStateFlow()

    override suspend fun refreshCounters() {
        withContext(ioDispatcher) {
            val terminal = try {
                remote.terminal()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                return@withContext
            }

            _counters.value = TerminalCounters(
                unreadTransactions = terminal.transactionsNew,
                unreadPromoCodes = terminal.promoCodesNew,
            )
        }
    }

    override fun clearTransactionsCounter() {
        _counters.update { it.copy(unreadTransactions = 0) }
    }

    override fun clearPromoCodesCounter() {
        _counters.update { it.copy(unreadPromoCodes = 0) }
    }

    override suspend fun clearSession() {
        _counters.value = TerminalCounters()
        _transactions.value = emptyList()
        _promoCodes.value = emptyList()
    }

    override suspend fun openTransactions(): Boolean = withContext(ioDispatcher) {
        val loaded = try {
            remote.transactions()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            return@withContext false
        }

        _transactions.value = loaded.mapNotNull(TransactionDto::toDomain)
            .sortedByDescending(TransactionEvent::timestamp)
            .take(TRANSACTIONS_LIMIT)
        clearTransactionsCounter()
        true
    }

    override suspend fun openPromoCodes(): Boolean = withContext(ioDispatcher) {
        val loaded = try {
            remote.promoCodes()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            return@withContext false
        }

        _promoCodes.value = loaded.mapNotNull(PromoDto::toDomain)
        clearPromoCodesCounter()
        true
    }
}
