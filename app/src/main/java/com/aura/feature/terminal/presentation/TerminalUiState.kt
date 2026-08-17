package com.aura.feature.terminal.presentation

import androidx.compose.runtime.Immutable
import com.aura.feature.terminal.domain.model.TerminalCounters

@Immutable
data class TerminalUiState(
    val counters: TerminalCounters = TerminalCounters(),
    val hasUnreadNews: Boolean = false,
)

@Immutable
data class TerminalActions(
    val onMenuClick: () -> Unit = {},
    val onNewsClick: () -> Unit = {},
    val onTransactionsClick: () -> Unit = {},
    val onPromoCodesClick: () -> Unit = {},
)
