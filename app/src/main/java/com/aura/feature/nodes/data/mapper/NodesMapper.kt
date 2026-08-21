package com.aura.feature.nodes.data.mapper

import com.aura.feature.nodes.data.remote.dto.FriendDto
import com.aura.feature.nodes.data.remote.dto.NodesSnapshotDto
import com.aura.feature.nodes.data.remote.dto.SocialLinkDto
import com.aura.feature.nodes.domain.model.Friend
import com.aura.feature.nodes.domain.model.FriendStatus
import com.aura.feature.nodes.domain.model.InviteOffer
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.model.ReferralRewards
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.domain.model.SocialLink
import com.aura.feature.nodes.domain.model.SocialNetwork
import com.aura.feature.nodes.domain.model.TierRates

private const val INITIALS_LIMIT = 2

fun NodesSnapshotDto.toDomain(): NodesState = NodesState(
    handle = handle,
    hasUnreadNews = hasUnreadNews,
    invite = InviteOffer(
        code = inviteCode,
        link = inviteLink,
        quote = inviteQuote?.takeIf { it.isNotBlank() },
        shareText = inviteShareText?.takeIf { it.isNotBlank() },
    ),
    friendsJoined = friendsJoined,
    activeFriends = activeFriends,
    tier = tier.toTier() ?: ReferralTier.IDLE,
    tierRates = TierRates(
        sparkPercent = tierSparkPercent,
        withdrawalPercent = tierWithdrawalPercent,
    ),
    nextTier = nextTier?.toTier(),
    friendsToNextTier = friendsToNextTier,
    rewards = ReferralRewards(spark = earnedSpark, ion = earnedIon),
    friends = friends.map(FriendDto::toDomain),
    socials = socials.mapNotNull(SocialLinkDto::toDomain),
)

private fun FriendDto.toDomain(): Friend = Friend(
    id = id,
    name = name,
    handle = handle,
    initials = name.toInitials(),
    spark = spark,
    ion = ion,
    status = status.toStatus(),
)

private fun SocialLinkDto.toDomain(): SocialLink? {
    val known = network.toNetwork() ?: return null
    return SocialLink(network = known, webUrl = webUrl, appUrl = appUrl?.takeIf { it.isNotBlank() })
}

private fun String.toInitials(): String = split(' ', '.')
    .filter { it.isNotBlank() }
    .take(INITIALS_LIMIT)
    .map { it.first().uppercaseChar() }
    .joinToString(separator = "")

private fun String.toTier(): ReferralTier? =
    ReferralTier.entries.firstOrNull { it.name == uppercase() }

private fun String.toStatus(): FriendStatus =
    FriendStatus.entries.firstOrNull { it.name == uppercase() } ?: FriendStatus.INACTIVE

private fun String.toNetwork(): SocialNetwork? =
    SocialNetwork.entries.firstOrNull { it.name == uppercase() }
