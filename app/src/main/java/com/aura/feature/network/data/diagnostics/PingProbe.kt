package com.aura.feature.network.data.diagnostics

import com.aura.BuildConfig
import com.aura.core.common.IoDispatcher
import com.aura.core.network.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private const val FALLBACK_HOST = "3.127.248.37"

private const val DEFAULT_PORT = 80

private const val TLS_PORT = 443

private const val CONNECT_TIMEOUT_MILLIS = 3_000

private const val NANOS_IN_MILLI = 1_000_000.0

data class PingSample(
    val roundTripsMs: List<Double>,
    val attempts: Int,
) {
    val pingMs: Int? get() = roundTripsMs.minOrNull()?.roundToInt()

    val jitterMs: Int?
        get() {
            if (roundTripsMs.size < 2) return null
            val average = roundTripsMs.average()
            return roundTripsMs.sumOf { kotlin.math.abs(it - average) }
                .div(roundTripsMs.size)
                .roundToInt()
        }

    val packetLossPercent: Double
        get() = if (attempts == 0) 0.0
        else (attempts - roundTripsMs.size) * PERCENT / attempts

    private companion object {
        const val PERCENT = 100.0
    }
}

interface PingProbe {
    suspend fun measure(): Int?

    suspend fun sample(attempts: Int): PingSample
}

@Singleton
class SocketPingProbe @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PingProbe {

    private val endpoint: InetSocketAddress by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val uri = runCatching { URI(BuildConfig.API_BASE_URL) }.getOrNull()
        val host = uri?.host ?: FALLBACK_HOST
        val port = when {
            uri?.port != null && uri.port > 0 -> uri.port
            uri?.scheme.equals("https", ignoreCase = true) -> TLS_PORT
            else -> DEFAULT_PORT
        }
        InetSocketAddress(host, port)
    }

    override suspend fun measure(): Int? {
        if (!networkMonitor.current().isOnline) return null
        return sample(DEFAULT_ATTEMPTS).pingMs
    }

    override suspend fun sample(attempts: Int): PingSample = withContext(ioDispatcher) {
        if (!networkMonitor.current().isOnline) return@withContext PingSample(emptyList(), 0)

        val roundTrips = buildList {
            repeat(attempts) {
                connectOnce()?.let(::add)
            }
        }

        PingSample(roundTripsMs = roundTrips, attempts = attempts)
    }

    private fun connectOnce(): Double? = try {
        Socket().use { socket ->
            val startedAt = System.nanoTime()
            socket.connect(endpoint, CONNECT_TIMEOUT_MILLIS)
            (System.nanoTime() - startedAt) / NANOS_IN_MILLI
        }
    } catch (error: IOException) {
        null
    }

    private companion object {
        const val DEFAULT_ATTEMPTS = 4
    }
}
