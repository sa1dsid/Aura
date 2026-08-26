package com.aura.feature.nodes.data.mapper

import com.aura.feature.nodes.data.remote.dto.FriendDto
import com.aura.feature.nodes.data.remote.dto.NodesSnapshotDto
import com.aura.feature.nodes.data.remote.dto.SocialLinkDto
import com.aura.feature.nodes.domain.model.FriendStatus
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.domain.model.SocialNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NodesMapperTest {

    @Test
    fun `builds initials from name and surname letter`() {
        val friends = snapshot(
            friends = listOf(
                friend(name = "Alex K."),
                friend(name = "Maria T."),
                friend(name = "rachel s."),
            ),
        ).toDomain().friends

        assertEquals(listOf("AK", "MT", "RS"), friends.map { it.initials })
    }

    @Test
    fun `keeps server order of friends untouched`() {
        val order = listOf("INACTIVE", "EARNING", "SPARK_ONLY")
        val friends = snapshot(
            friends = order.mapIndexed { index, status ->
                friend(id = "f$index", status = status)
            },
        ).toDomain().friends

        assertEquals(
            listOf(FriendStatus.INACTIVE, FriendStatus.EARNING, FriendStatus.SPARK_ONLY),
            friends.map { it.status },
        )
    }

    @Test
    fun `active squad keeps only earning friends`() {
        val squad = snapshot(
            friends = listOf(
                friend(id = "a", status = "EARNING"),
                friend(id = "b", status = "SPARK_ONLY"),
                friend(id = "c", status = "EARNING"),
                friend(id = "d", status = "INACTIVE"),
            ),
        ).toDomain().activeSquad

        assertEquals(listOf("a", "c"), squad.map { it.id })
    }

    @Test
    fun `falls back to inactive on an unknown status`() {
        val friends = snapshot(friends = listOf(friend(status = "SOMETHING_NEW"))).toDomain().friends

        assertEquals(FriendStatus.INACTIVE, friends.single().status)
    }

    @Test
    fun `reads tiers and drops an unknown next tier`() {
        val state = snapshot(tier = "core_node", nextTier = "GALACTIC").toDomain()

        assertEquals(ReferralTier.CORE_NODE, state.tier)
        assertNull(state.nextTier)
    }

    @Test
    fun `skips social networks the app cannot render`() {
        val socials = snapshot(
            socials = listOf(
                SocialLinkDto("DISCORD", "https://discord.gg/ioaura", null),
                SocialLinkDto("MASTODON", "https://mastodon.social/@ioaura", null),
                SocialLinkDto("x", "https://x.com/ioaura", "  "),
            ),
        ).toDomain().socials

        assertEquals(listOf(SocialNetwork.DISCORD, SocialNetwork.X), socials.map { it.network })
        assertNull(socials.last().appUrl)
    }

    @Test
    fun `treats blank invite copy as missing`() {
        val invite = snapshot(quote = "   ", shareText = null).toDomain().invite

        assertNull(invite.quote)
        assertNull(invite.shareText)
    }

    @Test
    fun `reports an empty network when nobody joined`() {
        assertTrue(snapshot(friends = emptyList()).toDomain().isEmpty)
    }

    @Test
    fun `derives the next tier from the tier order when the server keeps quiet`() {
        val derived = listOf("IDLE", "ACTIVE_SIGNAL", "STABLE_LINK", "CORE_NODE")
            .map { snapshot(tier = it, nextTier = null).toDomain().nextTier }

        assertEquals(
            listOf(
                ReferralTier.ACTIVE_SIGNAL,
                ReferralTier.STABLE_LINK,
                ReferralTier.CORE_NODE,
                ReferralTier.IONIC_PRIME,
            ),
            derived,
        )
    }

    @Test
    fun `leaves the top tier without a next one`() {
        assertNull(snapshot(tier = "IONIC_PRIME", nextTier = null).toDomain().nextTier)
    }

    @Test
    fun `starts an unknown tier over from idle`() {
        val state = snapshot(tier = "galactic", nextTier = null).toDomain()

        assertEquals(ReferralTier.IDLE, state.tier)
        assertEquals(ReferralTier.ACTIVE_SIGNAL, state.nextTier)
    }

    @Test
    fun `hands the rates and the rewards over untouched`() {
        val state = snapshot().toDomain()

        assertEquals(15, state.tierRates.sparkPercent)
        assertEquals(5.0, state.tierRates.withdrawalPercent, 0.0)
        assertEquals(3_260L, state.rewards.spark)
        assertEquals(890L, state.rewards.ion)
        assertEquals(1, state.friendsToNextTier)
    }

    @Test
    fun `carries the invite offer as the server wrote it`() {
        val invite = snapshot(
            quote = "I collect gifts in IO Aura",
            shareText = "Join my node",
        ).toDomain().invite

        assertEquals("SYREX482", invite.code)
        assertEquals("https://ioaura.app/i/syrex", invite.link)
        assertEquals("I collect gifts in IO Aura", invite.quote)
        assertEquals("Join my node", invite.shareText)
    }

    private fun friend(
        id: String = "f1",
        name: String = "Daniel R.",
        status: String = "SPARK_ONLY",
    ) = FriendDto(
        id = id,
        name = name,
        handle = "danr",
        spark = 54_200,
        ion = 0,
        status = status,
    )

    private fun snapshot(
        friends: List<FriendDto> = emptyList(),
        socials: List<SocialLinkDto> = emptyList(),
        tier: String = "CORE_NODE",
        nextTier: String? = "IONIC_PRIME",
        quote: String? = null,
        shareText: String? = null,
    ) = NodesSnapshotDto(
        handle = "syrex",
        inviteCode = "SYREX482",
        inviteLink = "https://ioaura.app/i/syrex",
        inviteQuote = quote,
        inviteShareText = shareText,
        friendsJoined = friends.size,
        activeFriends = friends.count { it.status == "EARNING" },
        tier = tier,
        tierSparkPercent = 15,
        tierWithdrawalPercent = 5.0,
        nextTier = nextTier,
        friendsToNextTier = 1,
        nextThreshold = null,
        earnedSpark = 3_260,
        earnedIon = 890,
        friends = friends,
        socials = socials,
    )
}
