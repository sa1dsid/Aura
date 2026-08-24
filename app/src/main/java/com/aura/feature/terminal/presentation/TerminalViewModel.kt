package com.aura.feature.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.news.domain.repository.NewsRepository
import com.aura.feature.terminal.domain.repository.TerminalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val terminalRepository: TerminalRepository,
    newsRepository: NewsRepository,
) : ViewModel() {

    val uiState: StateFlow<TerminalUiState> =
        combine(
            terminalRepository.counters,
            newsRepository.hasUnread,
        ) { counters, hasUnreadNews ->
            TerminalUiState(counters = counters, hasUnreadNews = hasUnreadNews)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TerminalUiState(),
        )

    fun onScreenResumed() {
        viewModelScope.launch { terminalRepository.refreshCounters() }
    }

    fun onTransactionsOpened() {
        viewModelScope.launch { terminalRepository.openTransactions() }
    }

    fun onPromoCodesOpened() {
        viewModelScope.launch { terminalRepository.openPromoCodes() }
    }
}
