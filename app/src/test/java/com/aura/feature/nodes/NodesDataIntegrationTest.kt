package com.aura.feature.nodes

import com.aura.feature.nodes.domain.model.FriendStatus
import com.aura.feature.nodes.domain.model.ReferralTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NodesDataIntegrationTest : NodesTestCase() {

    @Test
    fun `the snapshot fills the counters, the tier and its rates`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                friendsJoined = 6,
                activeFriends = 2,
                tier = Nodes.TIER_CORE_NODE,
                sparkReferralPercent = 15,
                ionReferralPercentStage2 = 5,
                earnedSpark = "3260.750000",
                earnedIon = 890,
            ),
        )
        val (_, states) = screenOf(stack)

        val state = awaitContent(states).nodes

        assertEquals(6, state.friendsJoined)
        assertEquals(2, state.activeFriends)
        assertEquals(ReferralTier.CORE_NODE, state.tier)
        assertEquals(15, state.tierRates.sparkPercent)
        assertEquals(5.0, state.tierRates.withdrawalPercent, 0.0)
        assertEquals(3_260L, state.rewards.spark)
        assertEquals(890L, state.rewards.ion)
    }

    @Test
    fun `the handle in the top bar comes from the signed in account`() = nodes { stack ->
        stack.signIn(handle = "syrex")
        val (_, states) = screenOf(stack)

        assertEquals("syrex", awaitContent(states).nodes.handle)
    }

    @Test
    fun `without a session the handle stays empty`() = nodes(signedIn = false) { stack ->
        val (_, states) = screenOf(stack)

        assertEquals("", awaitContent(states).nodes.handle)
    }

    @Test
    fun `friends keep the server order with their own earnings`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                friendsJoined = 3,
                activeFriends = 1,
                friends = listOf(
                    Nodes.friend(
                        id = 1,
                        displayName = "Alex K.",
                        status = Nodes.STATUS_EARNING,
                        ownSpark = "12400.990000",
                        ownIon = 1_840,
                    ),
                    Nodes.friend(
                        id = 2,
                        displayName = "Daniel R.",
                        status = Nodes.STATUS_SPARK_ONLY,
                        ownSpark = "54200.000000",
                    ),
                    Nodes.friend(
                        id = 3,
                        displayName = "Peter V.",
                        status = Nodes.STATUS_INACTIVE,
                    ),
                ),
            ),
        )
        val (_, states) = screenOf(stack)

        val friends = awaitContent(states).nodes.friends

        assertEquals(listOf("1", "2", "3"), friends.map { it.id })
        assertEquals(
            listOf(FriendStatus.EARNING, FriendStatus.SPARK_ONLY, FriendStatus.INACTIVE),
            friends.map { it.status },
        )
        assertEquals(listOf(12_400L, 54_200L, 0L), friends.map { it.spark })
        assertEquals(listOf(1_840L, 0L, 0L), friends.map { it.ion })
    }

    @Test
    fun `a friend row carries the display name the server sent`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(friends = listOf(Nodes.friend(displayName = "Alex K."))),
        )
        val (_, states) = screenOf(stack)

        assertEquals("Alex K.", awaitContent(states).nodes.friends.single().name)
    }

    @Test
    fun `initials take the first letters of the first two words`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                friends = listOf(
                    Nodes.friend(id = 1, displayName = "Alex K."),
                    Nodes.friend(id = 2, displayName = "rachel s."),
                    Nodes.friend(id = 3, displayName = "Madonna"),
                    Nodes.friend(id = 4, displayName = "jean.luc picard"),
                    Nodes.friend(id = 5, displayName = " "),
                ),
            ),
        )
        val (_, states) = screenOf(stack)

        val initials = awaitContent(states).nodes.friends.map { it.initials }

        assertEquals(listOf("AK", "RS", "M", "JL", ""), initials)
    }

    @Test
    fun `an unknown tier falls back to idle`() = nodes { stack ->
        stack.server.always(NodesPaths.NODES, body = Nodes.snapshot(tier = "galactic"))
        val (_, states) = screenOf(stack)

        val state = awaitContent(states).nodes

        assertEquals(ReferralTier.IDLE, state.tier)
        assertEquals(ReferralTier.ACTIVE_SIGNAL, state.nextTier)
    }

    @Test
    fun `the next tier follows the tier order because the server never sends it`() =
        nodes { stack ->
            val expected = listOf(
                Nodes.TIER_IDLE to ReferralTier.ACTIVE_SIGNAL,
                Nodes.TIER_ACTIVE_SIGNAL to ReferralTier.STABLE_LINK,
                Nodes.TIER_STABLE_LINK to ReferralTier.CORE_NODE,
                Nodes.TIER_CORE_NODE to ReferralTier.IONIC_PRIME,
            )
            val (viewModel, states) = screenOf(stack)

            expected.forEach { (tier, next) ->
                stack.server.always(NodesPaths.NODES, body = Nodes.snapshot(tier = tier))
                viewModel.onScreenResumed()

                awaitNodes(states, "the next tier after $tier") { it.nextTier == next }
            }
        }

    @Test
    fun `the top tier reports no next tier`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(tier = Nodes.TIER_IONIC_PRIME, activeFriends = 3),
        )
        val (_, states) = screenOf(stack)

        val state = awaitContent(states).nodes

        assertEquals(ReferralTier.IONIC_PRIME, state.tier)
        assertNull(state.nextTier)
    }

    @Test
    fun `an unknown friend status counts as inactive`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(friends = listOf(Nodes.friend(status = "SOMETHING_NEW"))),
        )
        val (_, states) = screenOf(stack)

        assertEquals(FriendStatus.INACTIVE, awaitContent(states).nodes.friends.single().status)
    }

    @Test
    fun `an empty friend list leaves the network empty`() = nodes { stack ->
        val (_, states) = screenOf(stack)

        val state = awaitContent(states).nodes

        assertTrue(state.isEmpty)
        assertTrue(state.activeSquad.isEmpty())
    }

    @Test
    fun `the active squad keeps only the earning friends`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                friendsJoined = 4,
                activeFriends = 2,
                friends = listOf(
                    Nodes.friend(id = 1, status = Nodes.STATUS_EARNING),
                    Nodes.friend(id = 2, status = Nodes.STATUS_SPARK_ONLY),
                    Nodes.friend(id = 3, status = Nodes.STATUS_EARNING),
                    Nodes.friend(id = 4, status = Nodes.STATUS_INACTIVE),
                ),
            ),
        )
        val (_, states) = screenOf(stack)

        val state = awaitContent(states).nodes

        assertFalse(state.isEmpty)
        assertEquals(listOf("1", "3"), state.activeSquad.map { it.id })
    }

    @Test
    fun `earnings that are not numbers fall back to zero`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                earnedSpark = "unavailable",
                friends = listOf(Nodes.friend(ownSpark = "unavailable")),
            ),
        )
        val (_, states) = screenOf(stack)

        val state = awaitContent(states).nodes

        assertEquals(0L, state.rewards.spark)
        assertEquals(0L, state.friends.single().spark)
    }

    @Test
    fun `the withdrawal rate arrives as a whole percent`() = nodes { stack ->
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(
                tier = Nodes.TIER_STABLE_LINK,
                sparkReferralPercent = 10,
                ionReferralPercentStage2 = 2,
            ),
        )
        val (_, states) = screenOf(stack)

        val rates = awaitContent(states).nodes.tierRates

        assertEquals(10, rates.sparkPercent)
        assertEquals(2.0, rates.withdrawalPercent, 0.0)
    }
}
