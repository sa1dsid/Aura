package com.aura.feature.news.data.remote.dto

data class NewsItemDto(
    val id: String,
    val title: String,
    val body: String,
    val publishedAtMillis: Long,
    val read: Boolean,
)
