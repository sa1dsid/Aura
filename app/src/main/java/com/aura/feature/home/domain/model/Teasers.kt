package com.aura.feature.home.domain.model

data class Teasers(
    val bonusWithdrawal: BonusWithdrawalTeaser,
    val spark: SparkTeaser,
    val vpnCode: VpnCodeTeaser,
)

data class BonusWithdrawalTeaser(
    val completedSteps: Int,
    val totalSteps: Int,
) {
    val isComplete: Boolean get() = completedSteps >= totalSteps
}

data class SparkTeaser(
    val collected: Long,
    val target: Long,
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
