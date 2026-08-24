package com.aura.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.news.domain.repository.NewsRepository
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.terminal.domain.repository.TerminalRepository
import com.aura.feature.transactions.domain.model.TransactionFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val terminalRepository: TerminalRepository,
    newsRepository: NewsRepository,
    sessionStore: SessionStore,
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter.ALL)

    val uiState: StateFlow<TransactionsUiState> = combine(
        sessionStore.account,
        terminalRepository.transactions,
        filter,
        newsRepository.hasUnread,
    ) { account, loaded, selected, hasUnreadNews ->
        TransactionsUiState(
            handle = account?.handle,
            hasUnreadNews = hasUnreadNews,
            events = loaded,
            filter = selected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TransactionsUiState(),
    )

    init {
        viewModelScope.launch { terminalRepository.openTransactions() }
    }

    fun onFilterClick(selected: TransactionFilter) {
        filter.value = selected
    }
}
