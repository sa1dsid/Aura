package com.aura.feature.home.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.aura.R
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.MeshState
import com.aura.feature.home.domain.model.TestStartRejection

@Immutable
sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Content(
        val home: HomeState,
        val mesh: MeshState,
        val hasUnreadNews: Boolean = false,
    ) : HomeUiState
}

sealed interface HomeEvent {

    data class TestRejected(val rejection: TestStartRejection) : HomeEvent

    data class TestCompleted(val rewardIon: Int) : HomeEvent

    data object TestInterrupted : HomeEvent

    data object CooldownResumed : HomeEvent
}

@Immutable
data class HomeActions(
    val onMenuClick: () -> Unit = {},
    val onNewsClick: () -> Unit = {},
    val onBonusWithdrawalClick: () -> Unit = {},
    val onSparkClick: () -> Unit = {},
    val onVpnCodeClick: () -> Unit = {},
    val onConnectionBadgeClick: () -> Unit = {},
    val onMainButtonClick: () -> Unit = {},
    val onInviteClick: () -> Unit = {},
    val onTabSelected: (HomeTab) -> Unit = {},
)

enum class HomeTab(@field:StringRes val labelRes: Int) {
    HOME(R.string.nav_home),
    NODES(R.string.nav_nodes),
    IONI(R.string.nav_ioni),
    TERMINAL(R.string.nav_terminal),
    NETWORK(R.string.nav_network),
}
