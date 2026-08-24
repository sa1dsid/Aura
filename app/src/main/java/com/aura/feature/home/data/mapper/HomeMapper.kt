package com.aura.feature.home.data.mapper

import com.aura.core.api.dto.DashboardDto
import com.aura.core.config.FeatureFlags
import com.aura.core.network.NetworkStatus
import com.aura.feature.home.domain.model.BatteryOptimizationState
import com.aura.feature.home.domain.model.BonusWithdrawalTeaser
import com.aura.feature.home.domain.model.ConnectionState
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.InviteState
import com.aura.feature.home.domain.model.IonBalances
import com.aura.feature.home.domain.model.NodeStatus
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.SPARK_COUPON_THRESHOLD
import com.aura.feature.home.domain.model.SparkTeaser
import com.aura.feature.home.domain.model.SparkWindow
import com.aura.feature.home.domain.model.Teasers
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.VpnCodeTeaser
import com.aura.feature.nodes.domain.model.NodesState

private const val PERCENT_BASE = 100

fun DashboardDto.toDomain(
    session: TestSessionState,
    spark: SparkWindow,
    network: NetworkStatus,
    flags: FeatureFlags,
    nodes: NodesState?,
): HomeState = HomeState(
    balances = IonBalances(
        accrued = accruedIon,
        availableToWithdraw = availableToWithdrawIon,
        reservedBonus = reservedBonusIon,
    ),
    nodeStatus = NodeStatus(
        currentTier = node.tier.toNodeTier() ?: NodeTier.IDLE_NODE,
        referralRate = node.rateMultiplierPercent / PERCENT_BASE.toDouble(),
        progressToNext = node.progressCurrent.toLong(),
        progressTarget = node.progressTarget?.toLong(),
        nextTier = (node.tier.toNodeTier() ?: NodeTier.IDLE_NODE).next(),
        rateMultiplierPercent = node.rateMultiplierPercent,
        sparkReferralPercent = node.sparkReferralPercent,
    ),
    teasers = Teasers(
        bonusWithdrawal = BonusWithdrawalTeaser(
            completedSteps = bonus.completed,
            totalSteps = bonus.total,
            signalLockTaps = bonus.signalLock,
            networkSyncFriends = bonus.networkSync,
            fullUplinkDays = bonus.fullUplink,
            dataShareGb = bonus.dataShareGb,
            isDataShareSoon = bonus.dataShareSoon || !flags.dataShare,
            isBlinking = bonusTeaserShouldBlink,
        ),
        spark = SparkTeaser(
            collected = spark.balance.toLong(),
            target = SPARK_COUPON_THRESHOLD,
            issuedCoupons = sparkCoupon.issued,
            couponLimit = sparkCoupon.limit,
            isCampaignComplete = sparkCoupon.completed,
        ),
        vpnCode = VpnCodeTeaser(
            isEnabled = flags.vpnCode,
            tierGaugePercent = node.tierGaugePercent(),
            contributionPercent = 0,
        ),
    ),
    connection = ConnectionState(
        networkType = network.type,
        isVpnActive = network.isVpnActive,
        rewardIon = REWARD_ION,
    ),
    session = session,
    invite = InviteState(
        friendsJoined = nodes?.friendsJoined ?: 0,
        friendsTarget = nodes?.friendsToNextTier ?: 0,
        referralRatePercent = node.sparkReferralPercent,
        inviteLink = nodes?.invite?.link.orEmpty(),
    ),
    tapCount = tapCount,
    unreadNews = unreadNews,
    batteryOptimization = BatteryOptimizationState(
        shouldShow = batteryOptimization.shouldShow,
        isDisabled = batteryOptimization.optimizationDisabled,
    ),
)

private const val REWARD_ION = 20

private fun com.aura.core.api.dto.NodeStatusDto.tierGaugePercent(): Int {
    val target = progressTarget ?: return PERCENT_BASE
    if (target <= 0) return PERCENT_BASE
    return (progressCurrent * PERCENT_BASE / target).coerceIn(0, PERCENT_BASE)
}

private fun NodeTier.next(): NodeTier? = NodeTier.entries.getOrNull(ordinal + 1)

private const val TIER_IDLE = "IDLE"

private fun String.toNodeTier(): NodeTier? {
    val name = uppercase()
    if (name == TIER_IDLE) return NodeTier.IDLE_NODE
    return NodeTier.entries.firstOrNull { it.name == name }
}
