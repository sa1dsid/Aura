package com.aura.feature.home.data.mapper

import com.aura.core.api.dto.BatteryOptimizationDto
import com.aura.core.api.dto.DashboardDto
import com.aura.core.api.dto.NodeStatusDto
import com.aura.core.common.parseIsoMillis
import com.aura.core.config.FeatureFlags
import com.aura.core.network.NetworkStatus
import com.aura.feature.home.domain.model.BatteryOptimizationState
import com.aura.feature.home.domain.model.BonusWithdrawalTeaser
import com.aura.feature.home.domain.model.ConnectionState
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.InviteState
import com.aura.feature.home.domain.model.IonBalances
import com.aura.feature.home.domain.model.IoniCard
import com.aura.feature.home.domain.model.IoniState
import com.aura.feature.home.domain.model.NETWORK_SYNC_FRIENDS
import com.aura.feature.home.domain.model.NodeStatus
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.SPARK_COUPON_THRESHOLD
import com.aura.feature.home.domain.model.SparkTeaser
import com.aura.feature.home.domain.model.SparkWindow
import com.aura.feature.home.domain.model.TAP_REWARD_ION
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
            readyCode = sparkCoupon.readyCode?.takeIf(String::isNotBlank),
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
        rewardIon = TAP_REWARD_ION,
    ),
    session = session,
    invite = InviteState(
        activeFriends = bonus.networkSync.coerceIn(0, NETWORK_SYNC_FRIENDS),
        friendsTarget = NETWORK_SYNC_FRIENDS,
        referralRatePercent = node.sparkReferralPercent,
        inviteLink = nodes?.invite?.link.orEmpty(),
    ),
    tapCount = tapCount,
    ioni = IoniCard(
        state = ioniState.toIoniState(),
        lastCompletedTapAt = ioniLastCompletedTap?.parseIsoMillis(),
    ),
)

fun BatteryOptimizationDto.toDomain() = BatteryOptimizationState(
    shouldShow = shouldShow,
    isDisabled = optimizationDisabled,
)

private fun NodeStatusDto.tierGaugePercent(): Int {
    val target = progressTarget ?: return PERCENT_BASE
    if (target <= 0) return PERCENT_BASE
    return (progressCurrent * PERCENT_BASE / target).coerceIn(0, PERCENT_BASE)
}

private fun NodeTier.next(): NodeTier? = NodeTier.entries.getOrNull(ordinal + 1)

private fun String.toIoniState(): IoniState {
    val name = uppercase()
    return IoniState.entries.firstOrNull { it.name == name } ?: IoniState.COMING
}

private const val TIER_IDLE = "IDLE"

private fun String.toNodeTier(): NodeTier? {
    val name = uppercase()
    if (name == TIER_IDLE) return NodeTier.IDLE_NODE
    return NodeTier.entries.firstOrNull { it.name == name }
}
