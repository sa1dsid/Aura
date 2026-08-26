package com.aura.core.api.dto

import com.aura.core.api.serialization.DecimalAsDoubleSerializer
import com.aura.core.api.serialization.DecimalAsLongSerializer
import com.aura.core.api.serialization.DecimalAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsDto(
    val id: Int,
    val title: String,
    val body: String,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("is_read") val isRead: Boolean = false,
)

@Serializable
data class UnreadNewsDto(
    val unread: Int = 0,
)

@Serializable
data class NodesDto(
    @SerialName("friends_joined") val friendsJoined: Int = 0,
    @SerialName("active_friends") val activeFriends: Int = 0,
    val tier: String = "idle",
    @SerialName("next_threshold") val nextThreshold: Int? = null,
    @SerialName("more_for_next_tier") val moreForNextTier: Int? = null,
    @SerialName("node_status") val nodeStatus: NodeStatusDto = NodeStatusDto(),
    @SerialName("spark_referral_percent") val sparkReferralPercent: Int = 0,
    @Serializable(DecimalAsDoubleSerializer::class)
    @SerialName("ion_referral_percent_stage_2") val ionReferralPercentStage2: Double = 0.0,
    @SerialName("earned_from_referrals") val earnedFromReferrals: ReferralEarningsDto =
        ReferralEarningsDto(),
    val friends: List<NodeFriendDto> = emptyList(),
)

@Serializable
data class ReferralEarningsDto(
    @Serializable(DecimalAsStringSerializer::class)
    val spark: String = "0",
    @Serializable(DecimalAsLongSerializer::class)
    val ion: Long = 0,
)

@Serializable
data class NodeFriendDto(
    val id: Int,
    @SerialName("display_name") val displayName: String,
    val status: String,
    @Serializable(DecimalAsStringSerializer::class)
    @SerialName("own_spark") val ownSpark: String = "0",
    @Serializable(DecimalAsLongSerializer::class)
    @SerialName("own_ion") val ownIon: Long = 0,
    @SerialName("tap_count") val tapCount: Int = 0,
)

@Serializable
data class TerminalDto(
    @SerialName("transactions_new") val transactionsNew: Int = 0,
    @SerialName("promo_codes_new") val promoCodesNew: Int = 0,
    @SerialName("feature_flags") val featureFlags: FeatureFlagsDto = FeatureFlagsDto(),
)

@Serializable
data class TransactionDto(
    val id: Int,
    val kind: String,
    val currency: String,
    @Serializable(DecimalAsStringSerializer::class)
    val amount: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class PromoDto(
    val id: Int,
    val code: String,
    val campaign: String,
    @SerialName("created_at") val createdAt: String,
)
