package com.aura.feature.home.domain.model

data class IonBalances(
    val accrued: Long,
    val availableToWithdraw: Long,
    val reservedBonus: Long,
)

data class InviteState(
    val friendsJoined: Int,
    val friendsTarget: Int,
    val referralRatePercent: Int,
    val inviteLink: String,
)

data class BatteryOptimizationState(
    val shouldShow: Boolean = false,
    val isDisabled: Boolean = false,
)

data class HomeState(
    val balances: IonBalances,
    val nodeStatus: NodeStatus,
    val teasers: Teasers,
    val connection: ConnectionState,
    val session: TestSessionState,
    val invite: InviteState,
    val tapCount: Int,
    val batteryOptimization: BatteryOptimizationState = BatteryOptimizationState(),
    val ioni: IoniCard = IoniCard(),
)
