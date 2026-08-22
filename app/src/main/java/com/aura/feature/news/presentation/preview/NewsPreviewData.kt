package com.aura.feature.news.presentation.preview

import com.aura.feature.news.data.mapper.toDomain
import com.aura.feature.news.data.remote.NewsSampleFeed
import com.aura.feature.news.presentation.NewsUiState

object NewsPreviewData {

    val items = NewsSampleFeed.map { it.toDomain() }

    val state = NewsUiState(items = items)
}
