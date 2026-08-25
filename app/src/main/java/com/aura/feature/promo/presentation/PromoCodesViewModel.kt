package com.aura.feature.promo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.common.LoadStatus
import com.aura.feature.news.domain.repository.NewsRepository
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.terminal.domain.repository.TerminalRepository
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
class PromoCodesViewModel @Inject constructor(
    private val terminalRepository: TerminalRepository,
    newsRepository: NewsRepository,
    sessionStore: SessionStore,
) : ViewModel() {

    private val status = MutableStateFlow(LoadStatus.LOADING)

    private var loadJob: Job? = null

    val uiState: StateFlow<PromoCodesUiState> = combine(
        sessionStore.account,
        terminalRepository.promoCodes,
        newsRepository.hasUnread,
        status,
    ) { account, loaded, hasUnreadNews, loadStatus ->
        PromoCodesUiState(
            handle = account?.handle,
            hasUnreadNews = hasUnreadNews,
            codes = loaded,
            status = loadStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PromoCodesUiState(),
    )

    fun onScreenResumed() = load()

    fun onRetryClick() = load()

    private fun load() {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            status.value = LoadStatus.LOADING
            status.value = if (terminalRepository.openPromoCodes()) {
                LoadStatus.READY
            } else {
                LoadStatus.FAILED
            }
        }
    }
}
