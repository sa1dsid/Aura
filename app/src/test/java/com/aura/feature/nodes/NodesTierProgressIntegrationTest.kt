package com.aura.feature.nodes

import org.junit.Assert.assertEquals
import org.junit.Test

class NodesTierProgressIntegrationTest : NodesTestCase() {

    @Test
    fun `the server count of friends left wins over everything else`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                activeFriends = 2,
                moreForNextTier = 1,
                nextThreshold = 6,
                nodeStatus = Nodes.nodeStatus(progressCurrent = 1, progressTarget = 6),
            ),
        )
        val (_, states) = screenOf(stack)

        assertEquals(1, awaitContent(states).nodes.friendsToNextTier)
    }

    @Test
    fun `a zero from the server is a real answer, not a missing one`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                activeFriends = 0,
                moreForNextTier = 0,
                nextThreshold = 6,
                nodeStatus = Nodes.nodeStatus(progressCurrent = 1, progressTarget = 6),
            ),
        )
        val (_, states) = screenOf(stack)

        assertEquals(0, awaitContent(states).nodes.friendsToNextTier)
    }

    @Test
    fun `without that count the own tap progress fills the gap`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                activeFriends = 0,
                moreForNextTier = null,
                nextThreshold = 6,
                nodeStatus = Nodes.nodeStatus(progressCurrent = 4, progressTarget = 6),
            ),
        )
        val (_, states) = screenOf(stack)

        assertEquals(2, awaitContent(states).nodes.friendsToNextTier)
    }

    @Test
    fun `an overshot tap progress never turns negative`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                moreForNextTier = null,
                nodeStatus = Nodes.nodeStatus(progressCurrent = 9, progressTarget = 6),
            ),
        )
        val (_, states) = screenOf(stack)

        assertEquals(0, awaitContent(states).nodes.friendsToNextTier)
    }

    @Test
    fun `without tap progress the threshold counts down the active friends`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                activeFriends = 2,
                moreForNextTier = null,
                nextThreshold = 6,
                nodeStatus = Nodes.nodeStatus(progressCurrent = 4, progressTarget = null),
            ),
        )
        val (_, states) = screenOf(stack)

        assertEquals(4, awaitContent(states).nodes.friendsToNextTier)
    }

    @Test
    fun `an overshot threshold never turns negative`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                activeFriends = 5,
                moreForNextTier = null,
                nextThreshold = 2,
                nodeStatus = Nodes.nodeStatus(progressTarget = null),
            ),
        )
        val (_, states) = screenOf(stack)

        assertEquals(0, awaitContent(states).nodes.friendsToNextTier)
    }

    @Test
    fun `with nothing to count on the gap stays at zero`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                activeFriends = 2,
                moreForNextTier = null,
                nextThreshold = null,
                nodeStatus = Nodes.nodeStatus(progressTarget = null),
            ),
        )
        val (_, states) = screenOf(stack)

        assertEquals(0, awaitContent(states).nodes.friendsToNextTier)
    }

    @Test
    fun `a snapshot without any node fields still reaches the screen`() = nodes { stack ->
        stack.server.always(NodesPaths.NODES, body = "{}")
        val (_, states) = screenOf(stack)

        val state = awaitContent(states).nodes

        assertEquals(0, state.friendsJoined)
        assertEquals(0, state.activeFriends)
        assertEquals(0, state.friendsToNextTier)
        assertEquals(0, state.tierRates.sparkPercent)
        assertEquals(0L, state.rewards.spark)
        assertEquals(0L, state.rewards.ion)
    }
}
