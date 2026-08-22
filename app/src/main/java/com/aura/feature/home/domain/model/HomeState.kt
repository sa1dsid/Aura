package com.aura.feature.home.domain.model

data class IonBalances(
    val accrued: Long,
    val availableToWithdraw: Long,
)

data class InviteState(
    val friendsJoined: Int,
    val friendsTarget: Int,
    val referralRatePercent: Int,
    val inviteLink: String,
)

data class HomeState(
    val balances: IonBalances,
    val nodeStatus: NodeStatus,
    val teasers: Teasers,
    val connection: ConnectionState,
    val session: TestSessionState,
    val invite: InviteState,
)
