package com.aura.feature.nodes.data.repository

import com.aura.feature.nodes.FakeNodesRemoteDataSource
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.nodesSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class NodesRepositoryImplTest {

    @Test
    fun `nothing reaches the screen before the first refresh`() = runTest {
        val repository = repositoryOf(FakeNodesRemoteDataSource())

        val states = collect(repository)

        assertTrue(states.isEmpty())
    }

    @Test
    fun `a refresh publishes the mapped snapshot`() = runTest {
        val remote = FakeNodesRemoteDataSource(nodesSnapshot(friendsJoined = 6, activeFriends = 2))
        val repository = repositoryOf(remote)
        val states = collect(repository)

        repository.refresh()

        assertEquals(1, states.size)
        assertEquals(6, states.single().friendsJoined)
        assertEquals(2, states.single().activeFriends)
    }

    @Test
    fun `a failing remote is swallowed and nothing reaches the screen`() = runTest {
        val remote = FakeNodesRemoteDataSource().apply { failure = IOException("offline") }
        val repository = repositoryOf(remote)
        val states = collect(repository)

        repository.refresh()

        assertEquals(1, remote.calls)
        assertTrue(states.isEmpty())
    }

    @Test
    fun `a failing refresh keeps the snapshot that is already there`() = runTest {
        val remote = FakeNodesRemoteDataSource(nodesSnapshot(friendsJoined = 6))
        val repository = repositoryOf(remote)
        val states = collect(repository)
        repository.refresh()

        remote.failure = IOException("offline")
        repository.refresh()

        assertEquals(1, states.size)
        assertEquals(6, states.single().friendsJoined)
    }

    @Test
    fun `cancellation is never swallowed`() = runTest {
        val remote = FakeNodesRemoteDataSource().apply { failure = CancellationException("stop") }
        val repository = repositoryOf(remote)

        val thrown = runCatching { repository.refresh() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `closing the session drops the snapshot`() = runTest {
        val repository = repositoryOf(FakeNodesRemoteDataSource())
        repository.refresh()

        repository.clearSession()

        assertTrue(collect(repository).isEmpty())
    }

    @Test
    fun `a screen that arrives later still sees the cached snapshot`() = runTest {
        val remote = FakeNodesRemoteDataSource(nodesSnapshot(friendsJoined = 4))
        val repository = repositoryOf(remote)
        repository.refresh()

        val states = collect(repository)

        assertEquals(4, states.single().friendsJoined)
    }

    @Test
    fun `the fetch is handed to the io dispatcher`() = runTest {
        val io = CountingDispatcher(StandardTestDispatcher(testScheduler))
        val remote = FakeNodesRemoteDataSource()
        val repository = NodesRepositoryImpl(remote = remote, ioDispatcher = io)

        repository.refresh()

        assertEquals(1, remote.calls)
        assertTrue(io.dispatches > 0)
    }

    private fun TestScope.repositoryOf(
        remote: FakeNodesRemoteDataSource,
    ) = NodesRepositoryImpl(
        remote = remote,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private fun TestScope.collect(
        repository: NodesRepositoryImpl,
    ): List<NodesState> {
        val states = mutableListOf<NodesState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeNodes().collect { states += it }
        }
        return states
    }
}

private class CountingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {

    var dispatches = 0
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatches++
        delegate.dispatch(context, block)
    }
}
