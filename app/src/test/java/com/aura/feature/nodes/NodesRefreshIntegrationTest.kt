package com.aura.feature.nodes

import com.aura.feature.nodes.presentation.NodesUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SERVER_ERROR = 503

class NodesRefreshIntegrationTest : NodesTestCase() {

    @Test
    fun `the screen starts on loading`() = nodes { stack ->
        val (_, states) = screenOf(stack)

        awaitContent(states)

        assertEquals(NodesUiState.Loading, states.first())
    }

    @Test
    fun `creating the screen asks the server once`() = nodes { stack ->
        val (_, states) = screenOf(stack)
        awaitContent(states)

        assertEquals(1, stack.server.hits(NodesPaths.NODES))
    }

    @Test
    fun `coming back to the screen asks the server again`() = nodes { stack ->
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        viewModel.onScreenResumed()

        awaitRequest(stack, NodesPaths.NODES, count = 2)
    }

    @Test
    fun `a failing snapshot leaves the screen on loading`() = nodes { stack ->
        stack.server.always(NodesPaths.NODES, code = SERVER_ERROR, body = "{}")
        val (_, states) = screenOf(stack)

        awaitRequest(stack, NodesPaths.NODES)

        assertTrue(states.none { it is NodesUiState.Content })
    }

    @Test
    fun `a dropped connection is retried and the snapshot still arrives`() = nodes { stack ->
        stack.server.nextDropsConnection(NodesPaths.NODES)
        stack.server.always(NodesPaths.NODES, body = Nodes.snapshot(friendsJoined = 6))
        val (_, states) = screenOf(stack)

        assertEquals(6, awaitContent(states).nodes.friendsJoined)
        assertTrue(stack.server.hits(NodesPaths.NODES) >= 2)
    }

    @Test
    fun `a failing refresh keeps the snapshot already on the screen`() = nodes { stack ->
        stack.server.always(NodesPaths.NODES, body = Nodes.snapshot(friendsJoined = 6))
        val (viewModel, states) = screenOf(stack)
        assertEquals(6, awaitContent(states).nodes.friendsJoined)

        stack.server.always(NodesPaths.NODES, code = SERVER_ERROR, body = "{}")
        viewModel.onScreenResumed()
        awaitRequest(stack, NodesPaths.NODES, count = 2)

        assertEquals(6, awaitContent(states).nodes.friendsJoined)
    }

    @Test
    fun `a later refresh replaces the whole snapshot`() = nodes { stack ->
        stack.server.always(NodesPaths.NODES, body = Nodes.snapshot(friendsJoined = 1))
        val (viewModel, states) = screenOf(stack)
        assertEquals(1, awaitContent(states).nodes.friendsJoined)

        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                friendsJoined = 7,
                activeFriends = 3,
                tier = Nodes.TIER_IONIC_PRIME,
            ),
        )
        viewModel.onScreenResumed()

        val state = awaitNodes(states, "the second snapshot") { it.friendsJoined == 7 }

        assertEquals(3, state.activeFriends)
    }

    @Test
    fun `closing the session drops the cached snapshot`() = nodes { stack ->
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        stack.repository.clearSession()
        val fresh = stack.eventsOf(stack.repository.observeNodes())

        assertTrue(fresh.isEmpty())
        assertTrue(states.last() is NodesUiState.Content)

        viewModel.onScreenResumed()

        awaitEvent(fresh)
    }

    @Test
    fun `unread news lights up the planet in the top bar`() = nodes(news = unreadNews()) { stack ->
        val (_, states) = screenOf(stack)

        assertTrue(awaitContent(states).hasUnreadNews)
    }

    @Test
    fun `reading the news puts the planet out`() = nodes(news = unreadNews()) { stack ->
        val (_, states) = screenOf(stack)
        assertTrue(awaitContent(states).hasUnreadNews)

        stack.newsRepository.markAllRead()

        awaitUntil("the planet to go out") {
            (states.last() as? NodesUiState.Content)?.hasUnreadNews == false
        }
    }

    @Test
    fun `without news the planet stays dark`() = nodes { stack ->
        val (_, states) = screenOf(stack)

        assertFalse(awaitContent(states).hasUnreadNews)
    }
}
