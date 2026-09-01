package com.aura.feature.home.domain.model

const val SPARK_COUPON_THRESHOLD = 240_000L
const val NETWORK_SYNC_FRIENDS = 4

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
}

data class SparkTeaser(
    val collected: Long,
    val target: Long,
    val issuedCoupons: Int = 0,
    val couponLimit: Int = 0,
    val isCampaignComplete: Boolean = false,
    val readyCode: String? = null,
) {
    val isTargetReached: Boolean get() = collected >= target

    val isCodeReady: Boolean get() = !readyCode.isNullOrBlank()

    val progress: Float
        get() = if (target <= 0L) 0f else (collected.toDouble() / target).toFloat().coerceIn(0f, 1f)

    val percentToCode: Int get() = (progress * 100f).toInt()
}

data class VpnCodeTeaser(
    val isEnabled: Boolean,
    val tierGaugePercent: Int,
    val contributionPercent: Int,
) {
    val isCodeReady: Boolean
        get() = isEnabled && tierGaugePercent >= 100 && contributionPercent >= 100
}
