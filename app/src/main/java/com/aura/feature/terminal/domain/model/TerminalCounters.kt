package com.aura.feature.terminal.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class TerminalCounters(
    val unreadTransactions: Int = 0,
    val unreadPromoCodes: Int = 0,
)
