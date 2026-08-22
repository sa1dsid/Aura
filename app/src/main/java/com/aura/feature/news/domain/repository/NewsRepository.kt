package com.aura.feature.news.domain.repository

import com.aura.feature.news.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow

interface NewsRepository {

    val news: Flow<List<NewsItem>>

    val hasUnread: Flow<Boolean>

    suspend fun refresh()

    suspend fun markAllRead()
}
