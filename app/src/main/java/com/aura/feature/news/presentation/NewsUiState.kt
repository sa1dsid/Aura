package com.aura.feature.news.presentation

import androidx.compose.runtime.Immutable
import com.aura.feature.news.domain.model.NewsItem

@Immutable
data class NewsUiState(
    val items: List<NewsItem> = emptyList(),
    val expandedId: String? = null,
)

@Immutable
data class NewsActions(
    val onCardClick: (NewsItem) -> Unit = {},
    val onDismissRequest: () -> Unit = {},
)
