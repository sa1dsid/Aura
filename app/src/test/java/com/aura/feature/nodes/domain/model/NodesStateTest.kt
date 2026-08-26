package com.aura.feature.nodes.domain.model

import com.aura.feature.nodes.friendOf
import com.aura.feature.nodes.nodesState
import com.aura.feature.nodes.socialOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NodesStateTest {

    @Test
    fun `the active squad keeps the earning friends in the order they came`() {
        val state = nodesState(
            friends = listOf(
                friendOf(id = "a", status = FriendStatus.EARNING),
                friendOf(id = "b", status = FriendStatus.SPARK_ONLY),
                friendOf(id = "c", status = FriendStatus.EARNING),
                friendOf(id = "d", status = FriendStatus.INACTIVE),
            ),
        )

        assertEquals(listOf("a", "c"), state.activeSquad.map { it.id })
    }

    @Test
    fun `the squad frame disappears when nobody is earning`() {
        val state = nodesState(
            friends = listOf(
                friendOf(id = "a", status = FriendStatus.SPARK_ONLY),
                friendOf(id = "b", status = FriendStatus.INACTIVE),
            ),
        )

        assertTrue(state.activeSquad.isEmpty())
    }

    @Test
    fun `the empty state is about the friend list, not about the active ones`() {
        val nobody = nodesState(friends = emptyList())
        val onlyInactive = nodesState(
            activeFriends = 0,
            friends = listOf(friendOf(status = FriendStatus.INACTIVE)),
        )

        assertTrue(nobody.isEmpty)
        assertFalse(onlyInactive.isEmpty)
    }

    @Test
    fun `a social row opens when the config gave it a url`() {
        assertTrue(socialOf(webUrl = "https://discord.gg/ioaura").isOpenable)
    }

    @Test
    fun `a social row stays closed without a url`() {
        assertFalse(socialOf(webUrl = "").isOpenable)
        assertFalse(socialOf(webUrl = "   ").isOpenable)
    }

    @Test
    fun `the tier order is the one the next tier is derived from`() {
        assertEquals(
            listOf(
                ReferralTier.IDLE,
                ReferralTier.ACTIVE_SIGNAL,
                ReferralTier.STABLE_LINK,
                ReferralTier.CORE_NODE,
                ReferralTier.IONIC_PRIME,
            ),
            ReferralTier.entries.toList(),
        )
    }

    @Test
    fun `the network order is the one the social rows are drawn in`() {
        assertEquals(
            listOf(
                SocialNetwork.DISCORD,
                SocialNetwork.TELEGRAM,
                SocialNetwork.X,
                SocialNetwork.REDDIT,
                SocialNetwork.INSTAGRAM,
                SocialNetwork.SNAPCHAT,
            ),
            SocialNetwork.entries.toList(),
        )
    }
}
