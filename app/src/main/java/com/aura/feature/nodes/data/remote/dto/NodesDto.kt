package com.aura.feature.nodes.data.remote.dto

data class NodesSnapshotDto(
    val handle: String,
    val inviteCode: String,
    val inviteLink: String,
    val inviteQuote: String?,
    val inviteShareText: String?,
    val friendsJoined: Int,
    val activeFriends: Int,
    val tier: String,
    val tierSparkPercent: Int,
    val tierWithdrawalPercent: Double,
    val nextTier: String?,
    val friendsToNextTier: Int,
    val earnedSpark: Long,
    val earnedIon: Long,
    val friends: List<FriendDto>,
    val socials: List<SocialLinkDto>,
)

data class FriendDto(
    val id: String,
    val name: String,
    val handle: String,
    val spark: Long,
    val ion: Long,
    val status: String,
)

data class SocialLinkDto(
    val network: String,
    val webUrl: String,
    val appUrl: String?,
)
