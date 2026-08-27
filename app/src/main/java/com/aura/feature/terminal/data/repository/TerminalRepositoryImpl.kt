package com.aura.feature.terminal.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.core.common.logFailure
import com.aura.core.common.runCatchingRetried
import com.aura.core.session.SessionCache
import com.aura.feature.terminal.data.remote.TerminalRemoteDataSource
import com.aura.feature.terminal.domain.model.TerminalCounters
import com.aura.feature.terminal.domain.repository.TerminalRepository
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

    private val unread = MutableStateFlow(TerminalCounters())

    override val counters: Flow<TerminalCounters> = unread.asStateFlow()

    override suspend fun clearSession() {
        unread.value = TerminalCounters()
    }

    override suspend fun refreshCounters() {
        withContext(ioDispatcher) {
            val terminal = runCatchingRetried { remote.terminal() }
                .logFailure("terminal")
                .getOrNull()
                ?: return@withContext

            unread.value = TerminalCounters(
                unreadTransactions = terminal.transactionsNew,
                unreadPromoCodes = terminal.promoCodesNew,
            )
        }
    }

    override fun clearTransactionsCounter() {
        unread.update { it.copy(unreadTransactions = 0) }
    }

    override fun clearPromoCodesCounter() {
        unread.update { it.copy(unreadPromoCodes = 0) }
    }
}
