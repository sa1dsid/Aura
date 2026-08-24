package com.aura.feature.news.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.common.parseIsoMillis
import com.aura.feature.news.data.remote.dto.NewsItemDto
import javax.inject.Inject
import javax.inject.Singleton

interface NewsRemoteDataSource {

    suspend fun fetchNews(): List<NewsItemDto>

    suspend fun markAllRead(): Int
}

@Singleton
class ApiNewsRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : NewsRemoteDataSource {

    override suspend fun fetchNews(): List<NewsItemDto> = api.news().map { item ->
        NewsItemDto(
            id = item.id.toString(),
            title = item.title,
            body = item.body,
            publishedAtMillis = item.publishedAt.parseIsoMillis() ?: 0,
            read = item.isRead,
        )
    }

    override suspend fun markAllRead(): Int = api.openNews().unread
}
