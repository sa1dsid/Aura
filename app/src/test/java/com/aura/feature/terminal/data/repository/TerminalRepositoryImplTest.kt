package com.aura.feature.terminal.data.repository

import com.aura.feature.terminal.FakeTerminalRemoteDataSource
import com.aura.feature.terminal.domain.model.TerminalCounters
import com.aura.feature.terminal.terminalDto
import com.aura.testing.CountingDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalRepositoryImplTest {

    private val remote = FakeTerminalRemoteDataSource()

    @Test
    fun `the counters start quiet`() = runTest {
        assertEquals(TerminalCounters(), counters(repositoryOf()).single())
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

        remote.failure = IOException("offline")
        repository.refreshCounters()

        assertEquals(105, counters.last().unreadTransactions)
    }

    @Test
    fun `cancellation is never swallowed`() = runTest {
        remote.failure = CancellationException("stop")
        val repository = repositoryOf()

        val thrown = runCatching { repository.refreshCounters() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `clearing one counter leaves the other alone`() = runTest {
        remote.counters = terminalDto(transactionsNew = 105, promoCodesNew = 2)
        val repository = repositoryOf()
        val counters = counters(repository)
        repository.refreshCounters()

        repository.clearTransactionsCounter()
        assertEquals(TerminalCounters(0, 2), counters.last())

        repository.clearPromoCodesCounter()
        assertEquals(TerminalCounters(), counters.last())
    }

    @Test
    fun `closing the session drops the counters`() = runTest {
        remote.counters = terminalDto(transactionsNew = 105, promoCodesNew = 2)
        val repository = repositoryOf()
        val counters = counters(repository)
        repository.refreshCounters()

        repository.clearSession()

        assertEquals(TerminalCounters(), counters.last())
    }

    @Test
    fun `the request is handed to the io dispatcher`() = runTest {
        val io = CountingDispatcher(StandardTestDispatcher(testScheduler))
        val repository = TerminalRepositoryImpl(remote = remote, ioDispatcher = io)

        repository.refreshCounters()

        assertEquals(1, remote.calls)
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
}
