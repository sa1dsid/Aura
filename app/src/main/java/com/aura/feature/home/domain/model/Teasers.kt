package com.aura.feature.home.domain.model

const val BONUS_SIGNAL_LOCK_TAPS = 20

const val BONUS_NETWORK_SYNC_FRIENDS = 4

const val BONUS_FULL_UPLINK_DAYS = 10

const val BONUS_DATA_SHARE_GB = 5

const val SPARK_COUPON_THRESHOLD = 240_000L

data class Teasers(
    val bonusWithdrawal: BonusWithdrawalTeaser,
    val spark: SparkTeaser,
    val vpnCode: VpnCodeTeaser,
)

data class BonusWithdrawalTeaser(
    val completedSteps: Int,
    val totalSteps: Int,
    val signalLockTaps: Int = 0,
    val networkSyncFriends: Int = 0,
    val fullUplinkDays: Int = 0,
    val dataShareGb: Int = 0,
    val isDataShareSoon: Boolean = true,
    val isBlinking: Boolean = false,
) {
    val isComplete: Boolean get() = completedSteps >= totalSteps

    val signalLockTarget: Int get() = BONUS_SIGNAL_LOCK_TAPS

    val networkSyncTarget: Int get() = BONUS_NETWORK_SYNC_FRIENDS

    val fullUplinkTarget: Int get() = BONUS_FULL_UPLINK_DAYS

    val dataShareTarget: Int get() = BONUS_DATA_SHARE_GB
}

data class SparkTeaser(
    val collected: Long,
    val target: Long,
    val issuedCoupons: Int = 0,
    val couponLimit: Int = 0,
    val isCampaignComplete: Boolean = false,
) {
    val isCodeReady: Boolean get() = collected >= target
}

data class VpnCodeTeaser(
    val isEnabled: Boolean,
    val tierGaugePercent: Int,
    val contributionPercent: Int,
) {
    val isCodeReady: Boolean
        get() = isEnabled && tierGaugePercent >= 100 && contributionPercent >= 100
}
