package com.aura.feature.news

import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.news.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeNewsRepository(initial: List<NewsItem> = emptyList()) : NewsRepository {

    private val feed = MutableStateFlow(initial)

    private val unread = MutableStateFlow(initial.any { !it.read })

    var refreshCount = 0
        private set

    var markAllReadCount = 0
        private set

    override val news: Flow<List<NewsItem>> = feed

    override val hasUnread: Flow<Boolean> = unread

    override suspend fun refresh() {
        refreshCount++
    }

    override suspend fun markAllRead() {
        markAllReadCount++
        unread.value = false
        feed.value = feed.value.map { it.copy(read = true) }
    }
}
