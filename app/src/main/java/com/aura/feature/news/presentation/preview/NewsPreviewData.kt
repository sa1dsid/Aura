package com.aura.feature.news.presentation.preview

import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.news.presentation.NewsUiState
import java.util.Calendar

private fun publishedAt(day: Int): Long = Calendar.getInstance().apply {
    set(2026, Calendar.JULY, day, 12, 0, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

object NewsPreviewData {

    val items = listOf(
        NewsItem(
            id = "1",
            title = "Data Share is coming",
            body = "The traffic-sharing engine enters final testing. Node holders will be the " +
                "first to get access when it goes live.\n" +
                "What changes: your Node tier will start earning a percentage of your friends' " +
                "withdrawal balance, the VPN Code progress card unlocks, and the Terminal " +
                "screen becomes fully active.",
            publishedAt = publishedAt(22),
            read = false,
        ),
        NewsItem(
            id = "2",
            title = "Second coupon window opens",
            body = "Everyone who reached 240,000 Spark can now claim their coupon in SigmaDrop. " +
                "The window stays open until the next network sync.",
            publishedAt = publishedAt(15),
            read = true,
        ),
        NewsItem(
            id = "3",
            title = "Network update 1.2",
            body = "Ping history now shows your last 50 presses with VPN tags on every entry, " +
                "so a VPN hop never reads as a bad network day.",
            publishedAt = publishedAt(8),
            read = true,
        ),
    )

    val state = NewsUiState(items = items)
}
