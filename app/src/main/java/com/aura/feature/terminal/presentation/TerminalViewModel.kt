package com.aura.feature.terminal.presentation

import androidx.lifecycle.ViewModel
import com.aura.feature.terminal.domain.model.TerminalCounters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private val PENDING_COUNTERS = TerminalCounters(
    unreadTransactions = 105,
    unreadPromoCodes = 2,
)

@HiltViewModel
class TerminalViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState(counters = PENDING_COUNTERS))
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    fun onTransactionsOpened() {
        _uiState.update { it.copy(counters = it.counters.copy(unreadTransactions = 0)) }
    }

    fun onPromoCodesOpened() {
        _uiState.update { it.copy(counters = it.counters.copy(unreadPromoCodes = 0)) }
    }
}
