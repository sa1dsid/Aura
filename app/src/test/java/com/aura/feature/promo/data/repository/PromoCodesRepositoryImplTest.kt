package com.aura.feature.promo.data.repository

import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.terminal.FakePromoCodesRemoteDataSource
import com.aura.feature.terminal.promoDto
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
class PromoCodesRepositoryImplTest {

    private val remote = FakePromoCodesRemoteDataSource()

    @Test
    fun `the wallet starts empty`() = runTest {
        assertTrue(codes(repositoryOf()).single().isEmpty())
    }

    @Test
    fun `a load fills the wallet and reports success`() = runTest {
        remote.stored = listOf(promoDto(id = 1))
        val repository = repositoryOf()
        val codes = codes(repository)

        assertTrue(repository.load())

        assertEquals(listOf("1"), codes.last().map { it.id })
    }

    @Test
    fun `the codes keep the order the server sent them in`() = runTest {
        remote.stored = listOf(promoDto(id = 3), promoDto(id = 1), promoDto(id = 2))
        val repository = repositoryOf()
        val codes = codes(repository)

        repository.load()

        assertEquals(listOf("3", "1", "2"), codes.last().map { it.id })
    }

    @Test
    fun `a code the mapper cannot read never reaches the wallet`() = runTest {
        remote.stored = listOf(promoDto(id = 1, createdAt = "not-a-date"), promoDto(id = 2))
        val repository = repositoryOf()
        val codes = codes(repository)

        repository.load()

        assertEquals(listOf("2"), codes.last().map { it.id })
    }

    @Test
    fun `a stalled load is given a second chance before it gives up`() = runTest {
        remote.stored = listOf(promoDto(id = 1))
        remote.failOnce = true
        val repository = repositoryOf()
        val codes = codes(repository)

        assertTrue(repository.load())

        assertEquals(2, remote.calls)
        assertEquals(listOf("1"), codes.last().map { it.id })
    }

    @Test
    fun `a failing load reports the failure and keeps the wallet`() = runTest {
        remote.stored = listOf(promoDto(id = 1))
        val repository = repositoryOf()
        val codes = codes(repository)
        repository.load()

        remote.failure = IOException("offline")

        assertFalse(repository.load())
        assertEquals(listOf("1"), codes.last().map { it.id })
    }

    @Test
    fun `cancellation is never swallowed`() = runTest {
        remote.failure = CancellationException("stop")
        val repository = repositoryOf()

        val thrown = runCatching { repository.load() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `closing the session drops the wallet`() = runTest {
        remote.stored = listOf(promoDto(id = 1))
        val repository = repositoryOf()
        val codes = codes(repository)
        repository.load()

        repository.clearSession()

        assertTrue(codes.last().isEmpty())
    }

    @Test
    fun `the request is handed to the io dispatcher`() = runTest {
        val io = CountingDispatcher(StandardTestDispatcher(testScheduler))
        val repository = PromoCodesRepositoryImpl(remote = remote, ioDispatcher = io)

        repository.load()

        assertEquals(1, remote.calls)
        assertTrue(io.dispatches > 0)
    }

    private fun TestScope.repositoryOf() = PromoCodesRepositoryImpl(
        remote = remote,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private fun TestScope.codes(repository: PromoCodesRepositoryImpl): List<List<PromoCode>> {
        val collected = mutableListOf<List<PromoCode>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.promoCodes.collect { collected += it }
        }
        return collected
    }
}
