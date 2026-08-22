package com.aura.feature.news.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.news.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
) : ViewModel() {

    private val opened = MutableStateFlow(emptyList<NewsItem>())

    private val expandedId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NewsUiState> = combine(opened, expandedId) { items, expanded ->
        NewsUiState(items = items, expandedId = expanded)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = NewsUiState(),
    )

    fun onDrawerOpened() {
        viewModelScope.launch {
            opened.value = repository.news.first()
            repository.refresh()
            opened.value = repository.news.first()
            repository.markAllRead()
        }
    }

    fun onDrawerDismissed() {
        expandedId.value = null
    }

    fun onCardClick(item: NewsItem) {
        expandedId.update { current -> item.id.takeIf { it != current } }
    }
}
