package com.aura.feature.nodes.presentation

import androidx.compose.runtime.Immutable
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.model.SocialLink

@Immutable
sealed interface NodesUiState {

    data object Loading : NodesUiState

    data class Content(val nodes: NodesState) : NodesUiState
}

@Immutable
data class NodesActions(
    val onMenuClick: () -> Unit = {},
    val onNewsClick: () -> Unit = {},
    val onCodeClick: () -> Unit = {},
    val onShareClick: () -> Unit = {},
    val onSocialClick: (SocialLink) -> Unit = {},
)
