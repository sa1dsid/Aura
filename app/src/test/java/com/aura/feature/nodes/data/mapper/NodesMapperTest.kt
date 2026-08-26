package com.aura.feature.nodes.data.mapper

import com.aura.core.api.dto.InviteStateDto
import com.aura.core.api.dto.NodesDto
import com.aura.core.config.AppConfig
import com.aura.feature.nodes.Nodes
import com.aura.feature.nodes.accountOf
import com.aura.feature.nodes.configOf
import com.aura.feature.nodes.domain.model.FriendStatus
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.domain.model.SocialNetwork
import com.aura.feature.nodes.friendDto
import com.aura.feature.nodes.inviteDto
import com.aura.feature.nodes.nodesDto
import com.aura.feature.onboarding.domain.model.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.aura.core.config.SocialNetwork as ConfigSocialNetwork

class NodesMapperTest {

    @Test
    fun `builds initials from name and surname letter`() {
        val friends = map(
            nodesDto(
                friends = listOf(
                    friendDto(id = 1, displayName = "Alex K."),
                    friendDto(id = 2, displayName = "Maria T."),
                    friendDto(id = 3, displayName = "rachel s."),
                ),
            )
        ).friends

        assertEquals(listOf("AK", "MT", "RS"), friends.map { it.initials })
    }

    @Test
    fun `keeps server order of friends untouched`() {
        val order = listOf(
            Nodes.STATUS_INACTIVE,
            Nodes.STATUS_EARNING,
            Nodes.STATUS_SPARK_ONLY,
        )
        val friends = map(
            nodesDto(
                friends = order.mapIndexed { index, status ->
                    friendDto(id = index, status = status)
                },
            )
        ).friends

        assertEquals(
            listOf(FriendStatus.INACTIVE, FriendStatus.EARNING, FriendStatus.SPARK_ONLY),
            friends.map { it.status },
        )
    }

    @Test
    fun `active squad keeps only earning friends`() {
        val squad = map(
            nodesDto(
                friends = listOf(
                    friendDto(id = 1, status = Nodes.STATUS_EARNING),
                    friendDto(id = 2, status = Nodes.STATUS_SPARK_ONLY),
                    friendDto(id = 3, status = Nodes.STATUS_EARNING),
                    friendDto(id = 4, status = Nodes.STATUS_INACTIVE),
                ),
            )
        ).activeSquad

        assertEquals(listOf("1", "3"), squad.map { it.id })
    }

    @Test
    fun `falls back to inactive on an unknown status`() {
        val friends = map(nodesDto(friends = listOf(friendDto(status = "SOMETHING_NEW")))).friends

        assertEquals(FriendStatus.INACTIVE, friends.single().status)
    }

    @Test
    fun `reads the tier whatever case the server used`() {
        assertEquals(ReferralTier.CORE_NODE, map(nodesDto(tier = "core_node")).tier)
        assertEquals(ReferralTier.CORE_NODE, map(nodesDto(tier = "CORE_NODE")).tier)
    }

    @Test
    fun `starts an unknown tier over from idle`() {
        val state = map(nodesDto(tier = "galactic"))

        assertEquals(ReferralTier.IDLE, state.tier)
        assertEquals(ReferralTier.ACTIVE_SIGNAL, state.nextTier)
    }

    @Test
    fun `derives the next tier from the tier order`() {
        val derived = listOf(
            Nodes.TIER_IDLE,
            Nodes.TIER_ACTIVE_SIGNAL,
            Nodes.TIER_STABLE_LINK,
            Nodes.TIER_CORE_NODE,
        ).map { map(nodesDto(tier = it)).nextTier }

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
        assertNull(map(nodesDto(tier = Nodes.TIER_IONIC_PRIME)).nextTier)
    }

    @Test
    fun `draws every network the screen knows, configured or not`() {
        val socials = map(
            config = configOf(
                ConfigSocialNetwork.DISCORD to "https://discord.gg/ioaura",
                ConfigSocialNetwork.YOUTUBE to "https://youtube.com/@ioaura",
                ConfigSocialNetwork.X to "   ",
            )
        ).socials

        assertEquals(SocialNetwork.entries.toList(), socials.map { it.network })
        assertTrue(socials.single { it.network == SocialNetwork.DISCORD }.isOpenable)
        assertTrue(socials.filter { it.network != SocialNetwork.DISCORD }.none { it.isOpenable })
    }

    @Test
    fun `carries the invite offer as the server wrote it`() {
        val invite = map(invite = inviteDto()).invite

        assertEquals(Nodes.PERSONAL_CODE, invite.code)
        assertEquals(Nodes.PERSONAL_URL, invite.link)
        assertEquals(Nodes.SHARE_TEXT, invite.shareText)
    }

    @Test
    fun `treats blank invite copy as missing`() {
        assertNull(map(invite = inviteDto(shareText = "   ")).invite.shareText)
    }

    @Test
    fun `falls back to the account link when the server has no invite`() {
        val invite = map(invite = null).invite

        assertEquals("", invite.code)
        assertEquals(Nodes.ACCOUNT_LINK, invite.link)
        assertNull(invite.shareText)
    }

    @Test
    fun `falls back to the account link when the personal url is blank`() {
        assertEquals(Nodes.ACCOUNT_LINK, map(invite = inviteDto(personalUrl = "")).invite.link)
    }

    @Test
    fun `takes the handle from the account`() {
        assertEquals("syrex", map(account = accountOf(handle = "syrex")).handle)
        assertEquals("", map(account = null).handle)
    }

    @Test
    fun `hands the rates and the rewards over untouched`() {
        val state = map(
            nodesDto(
                sparkReferralPercent = 10,
                ionReferralPercentStage2 = 2.5,
                earnedSpark = "3260.750000",
                earnedIon = 890,
            )
        )

        assertEquals(10, state.tierRates.sparkPercent)
        assertEquals(2.5, state.tierRates.withdrawalPercent, 0.0)
        assertEquals(3_260L, state.rewards.spark)
        assertEquals(890L, state.rewards.ion)
    }

    @Test
    fun `reports an empty network when nobody joined`() {
        assertTrue(map(nodesDto(friends = emptyList())).isEmpty)
    }

    private fun map(
        dto: NodesDto = nodesDto(),
        account: Account? = accountOf(),
        invite: InviteStateDto? = inviteDto(),
        config: AppConfig = AppConfig(),
    ) = dto.toDomain(account = account, invite = invite, config = config)
}
