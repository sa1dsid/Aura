package com.aura.feature.news.data.repository

import com.aura.core.common.ApplicationScope
import com.aura.core.common.IoDispatcher
import com.aura.feature.news.data.mapper.toDomain
import com.aura.feature.news.data.remote.NewsRemoteDataSource
import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.news.domain.repository.NewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val remote: NewsRemoteDataSource,
    @param:ApplicationScope scope: CoroutineScope,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NewsRepository {

    private val feed = MutableStateFlow(emptyList<NewsItem>())

    private val unread = MutableStateFlow(false)

    override val news: Flow<List<NewsItem>> = feed.asStateFlow()

    override val hasUnread: Flow<Boolean> = unread.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            try {
                val loaded = remote.fetchNews().map { it.toDomain() }
                feed.value = loaded.sortedByDescending(NewsItem::publishedAt)
                unread.value = loaded.any { !it.read }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
            }
        }
    }

    override suspend fun markAllRead() {
        unread.value = false
        withContext(ioDispatcher) {
            try {
                unread.value = remote.markAllRead() > 0
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
            }
        }
    }
}
