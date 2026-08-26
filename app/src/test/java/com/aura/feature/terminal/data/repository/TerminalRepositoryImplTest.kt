package com.aura.feature.terminal.data.repository

import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.terminal.FakeTerminalRemoteDataSource
import com.aura.feature.terminal.Terminal
import com.aura.feature.terminal.domain.model.TerminalCounters
import com.aura.feature.terminal.promoDto
import com.aura.feature.terminal.terminalDto
import com.aura.feature.terminal.transactionDto
import com.aura.feature.transactions.domain.model.TRANSACTIONS_LIMIT
import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.testing.CountingDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalRepositoryImplTest {

    private val remote = FakeTerminalRemoteDataSource()

    @Test
    fun `the counters start quiet`() = runTest {
        val counters = counters(repositoryOf())

        assertEquals(TerminalCounters(), counters.single())
    }

    @Test
    fun `a refresh publishes both counters`() = runTest {
        remote.counters = terminalDto(transactionsNew = 105, promoCodesNew = 2)
        val repository = repositoryOf()
        val counters = counters(repository)

        repository.refreshCounters()

        assertEquals(TerminalCounters(105, 2), counters.last())
    }

    @Test
    fun `a failing refresh keeps the counters that are there`() = runTest {
        remote.counters = terminalDto(transactionsNew = 105)
        val repository = repositoryOf()
        val counters = counters(repository)
        repository.refreshCounters()

        remote.countersFailure = IOException("offline")
        repository.refreshCounters()

        assertEquals(105, counters.last().unreadTransactions)
    }

    @Test
    fun `cancellation is never swallowed on a refresh`() = runTest {
        remote.countersFailure = CancellationException("stop")
        val repository = repositoryOf()

        val thrown = runCatching { repository.refreshCounters() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `opening the transactions fills the log and clears its counter`() = runTest {
        remote.counters = terminalDto(transactionsNew = 105, promoCodesNew = 2)
        remote.storedTransactions = listOf(transactionDto(id = 1))
        val repository = repositoryOf()
        val counters = counters(repository)
        val events = transactions(repository)
        repository.refreshCounters()

        assertTrue(repository.openTransactions())

        assertEquals(listOf("1"), events.last().map { it.id })
        assertEquals(0, counters.last().unreadTransactions)
        assertEquals(2, counters.last().unreadPromoCodes)
    }

    @Test
    fun `the log arrives newest first and capped`() = runTest {
        remote.storedTransactions = (1..TRANSACTIONS_LIMIT + 5).map { index ->
            transactionDto(id = index, createdAt = Terminal.isoOf(index))
        }
        val repository = repositoryOf()
        val events = transactions(repository)

        repository.openTransactions()

        assertEquals(TRANSACTIONS_LIMIT, events.last().size)
        assertEquals("105", events.last().first().id)
        assertEquals("6", events.last().last().id)
    }

    @Test
    fun `a failing open reports the failure and keeps the log`() = runTest {
        remote.storedTransactions = listOf(transactionDto(id = 1))
        val repository = repositoryOf()
        val events = transactions(repository)
        repository.openTransactions()

        remote.transactionsFailure = IOException("offline")

        assertFalse(repository.openTransactions())
        assertEquals(listOf("1"), events.last().map { it.id })
    }

    @Test
    fun `cancellation is never swallowed on an open`() = runTest {
        remote.transactionsFailure = CancellationException("stop")
        val repository = repositoryOf()

        val thrown = runCatching { repository.openTransactions() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `opening the promo codes fills the wallet and clears its counter`() = runTest {
        remote.counters = terminalDto(transactionsNew = 105, promoCodesNew = 2)
        remote.storedPromoCodes = listOf(promoDto(id = 1))
        val repository = repositoryOf()
        val counters = counters(repository)
        val codes = promoCodes(repository)
        repository.refreshCounters()

        assertTrue(repository.openPromoCodes())

        assertEquals(listOf("1"), codes.last().map { it.id })
        assertEquals(0, counters.last().unreadPromoCodes)
        assertEquals(105, counters.last().unreadTransactions)
    }

    @Test
    fun `a failing promo open reports the failure and keeps the wallet`() = runTest {
        remote.storedPromoCodes = listOf(promoDto(id = 1))
        val repository = repositoryOf()
        val codes = promoCodes(repository)
        repository.openPromoCodes()

        remote.promoCodesFailure = IOException("offline")

        assertFalse(repository.openPromoCodes())
        assertEquals(listOf("1"), codes.last().map { it.id })
    }

    @Test
    fun `closing the session drops the counters, the log and the wallet`() = runTest {
        remote.counters = terminalDto(transactionsNew = 105, promoCodesNew = 2)
        remote.storedTransactions = listOf(transactionDto(id = 1))
        remote.storedPromoCodes = listOf(promoDto(id = 1))
        val repository = repositoryOf()
        val counters = counters(repository)
        val events = transactions(repository)
        val codes = promoCodes(repository)
        repository.refreshCounters()
        repository.openTransactions()
        repository.openPromoCodes()

        repository.clearSession()

        assertEquals(TerminalCounters(), counters.last())
        assertTrue(events.last().isEmpty())
        assertTrue(codes.last().isEmpty())
    }

    @Test
    fun `every request is handed to the io dispatcher`() = runTest {
        val io = CountingDispatcher(StandardTestDispatcher(testScheduler))
        val repository = TerminalRepositoryImpl(remote = remote, ioDispatcher = io)

        repository.refreshCounters()
        repository.openTransactions()
        repository.openPromoCodes()

        assertEquals(1, remote.countersCalls)
        assertEquals(1, remote.transactionsCalls)
        assertEquals(1, remote.promoCodesCalls)
        assertTrue(io.dispatches > 0)
    }

    private fun TestScope.repositoryOf() = TerminalRepositoryImpl(
        remote = remote,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private fun TestScope.counters(repository: TerminalRepositoryImpl): List<TerminalCounters> {
        val collected = mutableListOf<TerminalCounters>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.counters.collect { collected += it }
        }
        return collected
    }

    private fun TestScope.transactions(
        repository: TerminalRepositoryImpl,
    ): List<List<TransactionEvent>> {
        val collected = mutableListOf<List<TransactionEvent>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.transactions.collect { collected += it }
        }
        return collected
    }

    private fun TestScope.promoCodes(
        repository: TerminalRepositoryImpl,
    ): List<List<PromoCode>> {
        val collected = mutableListOf<List<PromoCode>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.promoCodes.collect { collected += it }
        }
        return collected
    }
}
