package com.aura.feature.nodes.data.mapper

import com.aura.core.api.dto.InviteStateDto
import com.aura.core.api.dto.NodeFriendDto
import com.aura.core.api.dto.NodesDto
import com.aura.core.config.AppConfig
import com.aura.feature.nodes.domain.model.Friend
import com.aura.feature.nodes.domain.model.FriendStatus
import com.aura.feature.nodes.domain.model.InviteOffer
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.model.ReferralRewards
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.domain.model.SocialLink
import com.aura.feature.nodes.domain.model.SocialNetwork
import com.aura.feature.nodes.domain.model.TierRates
import com.aura.feature.onboarding.domain.model.Account

private const val INITIALS_LIMIT = 2

fun NodesDto.toDomain(
    account: Account?,
    invite: InviteStateDto?,
    config: AppConfig,
): NodesState {
    val currentTier = tier.toTier() ?: ReferralTier.IDLE

    return NodesState(
        handle = account?.handle.orEmpty(),
        invite = InviteOffer(
            code = invite?.personalCode.orEmpty(),
            link = invite?.personalUrl?.takeIf { it.isNotBlank() }
                ?: account?.inviteLink.orEmpty(),
            shareText = invite?.shareText?.takeIf { it.isNotBlank() },
        ),
        friendsJoined = friendsJoined,
        activeFriends = activeFriends,
        tier = currentTier,
        tierRates = TierRates(
            sparkPercent = sparkReferralPercent,
            withdrawalPercent = ionReferralPercentStage2,
        ),
        nextTier = currentTier.next,
        friendsToNextTier = friendsLeftForNextTier(),
        rewards = ReferralRewards(
            spark = earnedFromReferrals.spark.toWholeAmount(),
            ion = earnedFromReferrals.ion,
        ),
        friends = friends.map(NodeFriendDto::toDomain),
        socials = config.toSocialLinks(),
    )
}

private fun NodesDto.friendsLeftForNextTier(): Int = moreForNextTier
    ?: nodeStatus.progressTarget?.minus(nodeStatus.progressCurrent)?.coerceAtLeast(0)
    ?: nextThreshold?.minus(activeFriends)?.coerceAtLeast(0)
    ?: 0

private fun NodeFriendDto.toDomain(): Friend = Friend(
    id = id.toString(),
    name = displayName,
    initials = displayName.toInitials(),
    spark = ownSpark.toWholeAmount(),
    ion = ownIon,
    status = status.toStatus(),
)

private fun AppConfig.toSocialLinks(): List<SocialLink> {
    val urls = socialLinks
        .filter { it.url.isNotBlank() }
        .associate { it.network.name to it.url }

    return SocialNetwork.entries.map { network ->
        SocialLink(network = network, webUrl = urls[network.name].orEmpty())
    }
}

private fun String.toWholeAmount(): Long = toBigDecimalOrNull()?.toLong() ?: 0

private fun String.toInitials(): String = split(' ', '.')
    .filter { it.isNotBlank() }
    .take(INITIALS_LIMIT)
    .map { it.first().uppercaseChar() }
    .joinToString(separator = "")

private fun String.toTier(): ReferralTier? =
    ReferralTier.entries.firstOrNull { it.name == uppercase() }

private fun String.toStatus(): FriendStatus =
    FriendStatus.entries.firstOrNull { it.name == uppercase() } ?: FriendStatus.INACTIVE
