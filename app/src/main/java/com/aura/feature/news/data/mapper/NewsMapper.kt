package com.aura.feature.news.data.mapper

import com.aura.feature.news.data.remote.dto.NewsItemDto
import com.aura.feature.news.domain.model.NewsItem

fun NewsItemDto.toDomain(): NewsItem = NewsItem(
    id = id,
    title = title,
    body = body,
    publishedAt = publishedAtMillis,
    read = read,
)
