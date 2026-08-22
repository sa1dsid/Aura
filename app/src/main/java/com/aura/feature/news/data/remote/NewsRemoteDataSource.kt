package com.aura.feature.news.data.remote

import com.aura.feature.news.data.remote.dto.NewsItemDto
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

interface NewsRemoteDataSource {

    suspend fun fetchNews(): List<NewsItemDto>

    suspend fun markAllRead(): Int
}

@Singleton
class MockNewsRemoteDataSource @Inject constructor() : NewsRemoteDataSource {

    private val allRead = AtomicBoolean(false)

    override suspend fun fetchNews(): List<NewsItemDto> {
        delay(NETWORK_DELAY_MILLIS)
        val read = allRead.get()
        return NewsSampleFeed.map { if (read) it.copy(read = true) else it }
    }

    override suspend fun markAllRead(): Int {
        delay(NETWORK_DELAY_MILLIS)
        allRead.set(true)
        return 0
    }

    private companion object {
        const val NETWORK_DELAY_MILLIS = 300L
    }
}

internal val NewsSampleFeed = listOf(
    NewsItemDto(
        id = "1",
        title = "Data Share is coming",
        body = "The traffic-sharing engine enters final testing. Node holders will be the " +
            "first to get access when it goes live.\n" +
            "What changes: your Node tier will start earning a percentage of your friends' " +
            "withdrawal balance, the VPN Code progress card unlocks, and the Terminal " +
            "screen becomes fully active.\n" +
            "No action needed from you — everything switches on automatically.",
        publishedAtMillis = publishedAt(22),
        read = false,
    ),
    NewsItemDto(
        id = "2",
        title = "Second coupon window opens",
        body = "Everyone who reached 240,000 Spark can now claim their coupon in SigmaDrop. " +
            "The window stays open until the next network sync.\n\n" +
            "After claiming, your Spark resets to zero and the next accrual cycle begins " +
            "with your next Home press.",
        publishedAtMillis = publishedAt(15),
        read = true,
    ),
    NewsItemDto(
        id = "3",
        title = "Network update 1.2",
        body = "Ping history now shows your last 50 presses with VPN tags on every entry, " +
            "so a VPN hop never reads as a bad network day.\n\n" +
            "Diagnostics stays free and unlimited — test your connection as often as you like.",
        publishedAtMillis = publishedAt(8),
        read = true,
    ),
)

private fun publishedAt(day: Int): Long = Calendar.getInstance().apply {
    set(2026, Calendar.JULY, day, 12, 0, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
