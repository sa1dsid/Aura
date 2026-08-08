package com.aura.feature.home.presentation

import androidx.compose.runtime.Immutable
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.MeshState

@Immutable
sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Content(
        val home: HomeState,
        val mesh: MeshState,
    ) : HomeUiState
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

enum class HomeTab(val label: String) {
    HOME("HOME"),
    NODES("NODES"),
    IONI("IONI"),
    TERMINAL("TERMINAL"),
    NETWORK("NETWORK"),
}
