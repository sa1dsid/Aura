package com.aura.feature.news.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class NewsItem(
    val id: String,
    val title: String,
    val body: String,
    val publishedAt: Long,
    val read: Boolean,
)
