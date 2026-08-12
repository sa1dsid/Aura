package com.aura.feature.home.data.mapper

import com.aura.feature.home.data.remote.dto.HomeSnapshotDto
import com.aura.feature.home.domain.model.BonusWithdrawalTeaser
import com.aura.feature.home.domain.model.ConnectionState
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.InviteState
import com.aura.feature.home.domain.model.IonBalances
import com.aura.core.network.NetworkType
import com.aura.feature.home.domain.model.NewsState
import com.aura.feature.home.domain.model.NodeStatus
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.SparkTeaser
import com.aura.feature.home.domain.model.Teasers
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.VpnCodeTeaser

fun HomeSnapshotDto.toDomain(session: TestSessionState): HomeState = HomeState(
    balances = IonBalances(
        accrued = accruedIon,
        availableToWithdraw = availableToWithdrawIon,
    ),
    nodeStatus = NodeStatus(
        currentTier = tier.toNodeTier() ?: NodeTier.IDLE_NODE,
        referralRate = referralRate,
        progressToNext = tierProgress,
        progressTarget = tierTarget,
        nextTier = nextTier?.toNodeTier(),
    ),
    teasers = Teasers(
        bonusWithdrawal = BonusWithdrawalTeaser(
            completedSteps = bonusStepsComplete,
            totalSteps = bonusStepsTotal,
        ),
        spark = SparkTeaser(
            collected = sparkCollected,
            target = sparkTarget,
        ),
        vpnCode = VpnCodeTeaser(
            isEnabled = vpnSaleEnabled,
            tierGaugePercent = vpnTierGaugePercent,
            contributionPercent = vpnContributionPercent,
        ),
    ),
    connection = ConnectionState(
        networkType = networkType.toNetworkType(),
        isVpnActive = vpnActive,
        rewardIon = testRewardIon,
    ),
    session = session,
    invite = InviteState(
        friendsJoined = friendsJoined,
        friendsTarget = friendsTarget,
        referralRatePercent = referralRatePercent,
        inviteLink = inviteLink,
    ),
    news = NewsState(hasUnread = hasUnreadNews),
)

private fun String.toNodeTier(): NodeTier? =
    NodeTier.entries.firstOrNull { it.name == this }

private fun String.toNetworkType(): NetworkType =
    NetworkType.entries.firstOrNull { it.name == this } ?: NetworkType.NONE
