package com.aura.feature.promo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.news.domain.repository.NewsRepository
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.promo.presentation.preview.PromoPreviewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class PromoCodesViewModel @Inject constructor(
    newsRepository: NewsRepository,
    sessionStore: SessionStore,
) : ViewModel() {

    private val codes = MutableStateFlow(PromoPreviewData.codes)

    val uiState: StateFlow<PromoCodesUiState> = combine(
        sessionStore.account,
        codes,
        newsRepository.hasUnread,
    ) { account, loaded, hasUnreadNews ->
        PromoCodesUiState(
            handle = account?.handle,
            hasUnreadNews = hasUnreadNews,
            codes = loaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PromoCodesUiState(),
    )
}
