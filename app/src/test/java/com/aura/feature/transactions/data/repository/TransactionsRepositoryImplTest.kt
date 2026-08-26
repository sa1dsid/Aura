package com.aura.feature.transactions.data.repository

import com.aura.feature.terminal.FakeTransactionsRemoteDataSource
import com.aura.feature.terminal.Terminal
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
class TransactionsRepositoryImplTest {

    private val remote = FakeTransactionsRemoteDataSource()

    @Test
    fun `the log starts empty`() = runTest {
        assertTrue(events(repositoryOf()).single().isEmpty())
    }

    @Test
    fun `a load fills the log and reports success`() = runTest {
        remote.stored = listOf(transactionDto(id = 1))
        val repository = repositoryOf()
        val events = events(repository)

        assertTrue(repository.load())

        assertEquals(listOf("1"), events.last().map { it.id })
    }

    @Test
    fun `the log arrives newest first and capped`() = runTest {
        remote.stored = (1..TRANSACTIONS_LIMIT + 5).map { index ->
            transactionDto(id = index, createdAt = Terminal.isoOf(index))
        }
        val repository = repositoryOf()
        val events = events(repository)

        repository.load()

        assertEquals(TRANSACTIONS_LIMIT, events.last().size)
        assertEquals("105", events.last().first().id)
        assertEquals("6", events.last().last().id)
    }

    @Test
    fun `rows the mapper cannot read never reach the log`() = runTest {
        remote.stored = listOf(
            transactionDto(id = 1, createdAt = "not-a-date"),
            transactionDto(id = 2, amount = "unavailable"),
            transactionDto(id = 3),
        )
        val repository = repositoryOf()
        val events = events(repository)

        repository.load()

        assertEquals(listOf("3"), events.last().map { it.id })
    }

    @Test
    fun `a failing load reports the failure and keeps the log`() = runTest {
        remote.stored = listOf(transactionDto(id = 1))
        val repository = repositoryOf()
        val events = events(repository)
        repository.load()

        remote.failure = IOException("offline")

        assertFalse(repository.load())
        assertEquals(listOf("1"), events.last().map { it.id })
    }

    @Test
    fun `cancellation is never swallowed`() = runTest {
        remote.failure = CancellationException("stop")
        val repository = repositoryOf()

        val thrown = runCatching { repository.load() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `closing the session drops the log`() = runTest {
        remote.stored = listOf(transactionDto(id = 1))
        val repository = repositoryOf()
        val events = events(repository)
        repository.load()

        repository.clearSession()

        assertTrue(events.last().isEmpty())
    }

    @Test
    fun `the request is handed to the io dispatcher`() = runTest {
        val io = CountingDispatcher(StandardTestDispatcher(testScheduler))
        val repository = TransactionsRepositoryImpl(remote = remote, ioDispatcher = io)

        repository.load()

        assertEquals(1, remote.calls)
        assertTrue(io.dispatches > 0)
    }

    private fun TestScope.repositoryOf() = TransactionsRepositoryImpl(
        remote = remote,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private fun TestScope.events(
        repository: TransactionsRepositoryImpl,
    ): List<List<TransactionEvent>> {
        val collected = mutableListOf<List<TransactionEvent>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.transactions.collect { collected += it }
        }
        return collected
    }
}
