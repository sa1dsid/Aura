package com.aura.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.common.LoadStatus
import com.aura.feature.news.domain.repository.NewsRepository
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.terminal.domain.repository.TerminalRepository
import com.aura.feature.transactions.domain.model.TransactionFilter
import com.aura.feature.transactions.domain.repository.TransactionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val transactionsRepository: TransactionsRepository,
    private val terminalRepository: TerminalRepository,
    newsRepository: NewsRepository,
    sessionStore: SessionStore,
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter.ALL)

    private val status = MutableStateFlow(LoadStatus.LOADING)

    private var loadJob: Job? = null

    val uiState: StateFlow<TransactionsUiState> = combine(
        sessionStore.account,
        transactionsRepository.transactions,
        filter,
        newsRepository.hasUnread,
        status,
    ) { account, loaded, selected, hasUnreadNews, loadStatus ->
        TransactionsUiState(
            handle = account?.handle,
            hasUnreadNews = hasUnreadNews,
            events = loaded,
            filter = selected,
            status = loadStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TransactionsUiState(),
    )

    fun onScreenResumed() = load()

    fun onRetryClick() = load()

    fun onFilterClick(selected: TransactionFilter) {
        filter.value = selected
    }

    private fun load() {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            status.value = LoadStatus.LOADING
            val loaded = transactionsRepository.load()
            if (loaded) terminalRepository.clearTransactionsCounter()
            status.value = if (loaded) LoadStatus.READY else LoadStatus.FAILED
        }
    }
}
