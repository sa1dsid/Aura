package com.aura.feature.nodes.domain.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.aura.R

enum class ReferralTier(@field:StringRes val labelRes: Int) {
    IDLE(R.string.tier_idle),
    ACTIVE_SIGNAL(R.string.tier_active_signal),
    STABLE_LINK(R.string.tier_stable_link),
    CORE_NODE(R.string.tier_core_node),
    IONIC_PRIME(R.string.tier_ionic_prime),
}

enum class FriendStatus { EARNING, SPARK_ONLY, INACTIVE }

@Immutable
data class Friend(
    val id: String,
    val name: String,
    val handle: String,
    val initials: String,
    val spark: Long,
    val ion: Long,
    val status: FriendStatus,
)

@Immutable
data class TierRates(
    val sparkPercent: Int,
    val withdrawalPercent: Double,
)

@Immutable
data class ReferralRewards(
    val spark: Long,
    val ion: Long,
)

@Immutable
data class InviteOffer(
    val code: String,
    val link: String,
    val quote: String?,
    val shareText: String?,
)

enum class SocialNetwork { DISCORD, TELEGRAM, X, REDDIT, INSTAGRAM, SNAPCHAT }

@Immutable
data class SocialLink(
    val network: SocialNetwork,
    val webUrl: String,
    val appUrl: String?,
)

@Immutable
data class NodesState(
    val handle: String,
    val hasUnreadNews: Boolean,
    val invite: InviteOffer,
    val friendsJoined: Int,
    val activeFriends: Int,
    val tier: ReferralTier,
    val tierRates: TierRates,
    val nextTier: ReferralTier?,
    val friendsToNextTier: Int,
    val rewards: ReferralRewards,
    val friends: List<Friend>,
    val socials: List<SocialLink>,
) {
    val isEmpty: Boolean get() = friends.isEmpty()

    val activeSquad: List<Friend> get() = friends.filter { it.status == FriendStatus.EARNING }
}
