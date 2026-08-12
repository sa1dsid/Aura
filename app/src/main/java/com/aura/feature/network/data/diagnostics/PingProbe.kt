package com.aura.feature.network.data.diagnostics

import com.aura.core.network.NetworkMonitor
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

interface PingProbe {
    suspend fun measure(): Int?
}

@Singleton
class MockPingProbe @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) : PingProbe {

    override suspend fun measure(): Int? {
        if (!networkMonitor.current().isOnline) return null
        delay(PROBE_MILLIS)

        val random = Random(System.nanoTime())
        val base = random.nextInt(MIN_PING_MS, MAX_PING_MS)
        return if (networkMonitor.current().isVpnActive) base + random.nextInt(VPN_OVERHEAD_MS) else base
    }

    private companion object {
        const val PROBE_MILLIS = 400L
        const val MIN_PING_MS = 16
        const val MAX_PING_MS = 48
        const val VPN_OVERHEAD_MS = 40
    }
}
