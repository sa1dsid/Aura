package com.aura.feature.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.news.domain.repository.NewsRepository
import com.aura.feature.terminal.domain.model.TerminalCounters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

private val PENDING_COUNTERS = TerminalCounters(
    unreadTransactions = 105,
    unreadPromoCodes = 2,
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    newsRepository: NewsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState(counters = PENDING_COUNTERS))

    val uiState: StateFlow<TerminalUiState> =
        combine(_uiState, newsRepository.hasUnread) { state, hasUnreadNews ->
            state.copy(hasUnreadNews = hasUnreadNews)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = _uiState.value,
        )

    fun onTransactionsOpened() {
        _uiState.update { it.copy(counters = it.counters.copy(unreadTransactions = 0)) }
    }

    fun onPromoCodesOpened() {
        _uiState.update { it.copy(counters = it.counters.copy(unreadPromoCodes = 0)) }
    }
}
