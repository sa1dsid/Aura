package com.aura.feature.home.domain.model

const val SPARK_COUPON_THRESHOLD = 240_000L
const val SIGNAL_LOCK_TAPS = 20
const val NETWORK_SYNC_FRIENDS = 4
const val FULL_UPLINK_DAYS = 10
const val DATA_SHARE_GB = 5
const val BONUS_TOTAL_STEPS = 4

data class Teasers(
    val bonusWithdrawal: BonusWithdrawalTeaser,
    val spark: SparkTeaser,
    val vpnCode: VpnCodeTeaser,
)

enum class BonusStep(val target: Int) {
    SIGNAL_LOCK(SIGNAL_LOCK_TAPS),
    NETWORK_SYNC(NETWORK_SYNC_FRIENDS),
    FULL_UPLINK(FULL_UPLINK_DAYS),
    DATA_SHARE(DATA_SHARE_GB),
}

sealed interface BonusStepPage {
    val step: BonusStep
    val completedSteps: Int

    data class Task(
        override val step: BonusStep,
        override val completedSteps: Int,
        val current: Int,
        val isLocked: Boolean,
    ) : BonusStepPage

    data class Done(
        override val step: BonusStep,
        override val completedSteps: Int,
    ) : BonusStepPage
}

data class BonusWithdrawalTeaser(
    val completedSteps: Int,
    val totalSteps: Int,
    val signalLockTaps: Int = 0,
    val networkSyncFriends: Int = 0,
    val fullUplinkDays: Int = 0,
    val dataShareGb: Int = 0,
    val isDataShareSoon: Boolean = true,
    val isBlinking: Boolean = false,
    val congratulatedSteps: Int = completedSteps,
) {
    val isComplete: Boolean get() = completedSteps >= totalSteps

    val hasPendingCongratulation: Boolean
        get() = completedSteps > congratulatedSteps && congratulatedSteps < BONUS_TOTAL_STEPS - 1

    fun openingPage(): BonusStepPage =
        if (hasPendingCongratulation) donePage(congratulatedSteps) else taskPage(completedSteps)

    fun currentOf(step: BonusStep): Int = when (step) {
        BonusStep.SIGNAL_LOCK -> signalLockTaps
        BonusStep.NETWORK_SYNC -> networkSyncFriends
        BonusStep.FULL_UPLINK -> fullUplinkDays
        BonusStep.DATA_SHARE -> dataShareGb
    }

    private fun donePage(index: Int): BonusStepPage.Done {
        val safe = index.coerceIn(0, BONUS_TOTAL_STEPS - 1)
        return BonusStepPage.Done(
            step = BonusStep.entries[safe],
            completedSteps = safe + 1,
        )
    }

    private fun taskPage(index: Int): BonusStepPage.Task {
        val safe = index.coerceIn(0, BONUS_TOTAL_STEPS - 1)
        val step = BonusStep.entries[safe]
        return BonusStepPage.Task(
            step = step,
            completedSteps = safe,
            current = currentOf(step).coerceIn(0, step.target),
            isLocked = step == BonusStep.DATA_SHARE && isDataShareSoon,
        )
    }
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
