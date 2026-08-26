package com.aura.feature.nodes.presentation.preview

import com.aura.feature.nodes.domain.model.Friend
import com.aura.feature.nodes.domain.model.FriendStatus
import com.aura.feature.nodes.domain.model.InviteOffer
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.model.ReferralRewards
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.domain.model.SocialLink
import com.aura.feature.nodes.domain.model.SocialNetwork
import com.aura.feature.nodes.domain.model.TierRates
import com.aura.feature.nodes.presentation.NodesUiState

object NodesPreviewData {

    private val invite = InviteOffer(
        code = "SYREX482",
        link = "https://ioaura.app/i/syrex",
        shareText = null,
    )

    private val socials = listOf(
        SocialLink(SocialNetwork.DISCORD, "https://discord.gg/ioaura"),
        SocialLink(SocialNetwork.TELEGRAM, "https://t.me/ioaura"),
        SocialLink(SocialNetwork.X, "https://x.com/ioaura"),
        SocialLink(SocialNetwork.REDDIT, "https://reddit.com/r/ioaura"),
        SocialLink(SocialNetwork.INSTAGRAM, "https://instagram.com/ioaura"),
        SocialLink(SocialNetwork.SNAPCHAT, "https://snapchat.com/add/ioaura"),
    )

    private val friends = listOf(
        Friend("f1", "Alex K.", "AK", 12_400, 1_840, FriendStatus.EARNING),
        Friend("f2", "Maria T.", "MT", 8_900, 640, FriendStatus.EARNING),
        Friend("f3", "Daniel R.", "DR", 54_200, 0, FriendStatus.SPARK_ONLY),
        Friend("f4", "Jenna L.", "JL", 3_100, 0, FriendStatus.SPARK_ONLY),
        Friend("f5", "Peter V.", "PV", 0, 0, FriendStatus.INACTIVE),
        Friend("f6", "Rachel S.", "RS", 0, 0, FriendStatus.INACTIVE),
    )

    private val coreNode = NodesState(
        handle = "syrex",
        invite = invite,
        friendsJoined = 6,
        activeFriends = 2,
        tier = ReferralTier.CORE_NODE,
        tierRates = TierRates(sparkPercent = 15, withdrawalPercent = 5.0),
        nextTier = ReferralTier.IONIC_PRIME,
        friendsToNextTier = 1,
        rewards = ReferralRewards(spark = 3_260, ion = 890),
        friends = friends,
        socials = socials,
    )

    val content: NodesUiState = NodesUiState.Content(coreNode)

    val prime: NodesUiState = NodesUiState.Content(
        coreNode.copy(
            activeFriends = 3,
            tier = ReferralTier.IONIC_PRIME,
            tierRates = TierRates(sparkPercent = 20, withdrawalPercent = 10.0),
            nextTier = null,
            friendsToNextTier = 0,
        )
    )

    val empty: NodesUiState = NodesUiState.Content(
        coreNode.copy(
            friendsJoined = 0,
            activeFriends = 0,
            tier = ReferralTier.ACTIVE_SIGNAL,
            tierRates = TierRates(sparkPercent = 10, withdrawalPercent = 2.5),
            nextTier = ReferralTier.STABLE_LINK,
            friendsToNextTier = 1,
            rewards = ReferralRewards(spark = 0, ion = 0),
            friends = emptyList(),
        )
    )
}
