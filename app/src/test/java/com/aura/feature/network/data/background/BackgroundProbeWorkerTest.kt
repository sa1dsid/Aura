package com.aura.feature.network.data.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.aura.core.auth.TokenStore
import com.aura.feature.home.MutableNetworkMonitor
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.repository.PingHistoryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class BackgroundProbeWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val networkMonitor = MutableNetworkMonitor()

    private val pingHistory = CountingPingHistory()

    private val tokenStore: TokenStore = mockk()

    private var token: String? = "header.payload.signature"

    init {
        coEvery { tokenStore.token() } answers { token }
    }

    @Test
    fun `an ordinary background window records one probe`() = runTest {
        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(listOf(PingSource.BACKGROUND), pingHistory.probes)
    }

    @Test
    fun `a window without a network is skipped in silence`() = runTest {
        networkMonitor.set(isOnline = false)

        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(pingHistory.probes.isEmpty())
    }

    @Test
    fun `a window behind a vpn is skipped in silence`() = runTest {
        networkMonitor.set(isVpnActive = true)

        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(pingHistory.probes.isEmpty())
    }

    @Test
    fun `a signed out device is never probed`() = runTest {
        token = null

        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(pingHistory.probes.isEmpty())
    }

    @Test
    fun `a probe that fails is asked for again later`() = runTest {
        pingHistory.error = IOException("offline")

        assertEquals(ListenableWorker.Result.retry(), worker().doWork())
    }

    private fun worker(): BackgroundProbeWorker =
        TestListenableWorkerBuilder<BackgroundProbeWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = BackgroundProbeWorker(
                        context = appContext,
                        parameters = workerParameters,
                        pingHistory = pingHistory,
                        networkMonitor = networkMonitor,
                        tokenStore = tokenStore,
                    )
                }
            )
            .build()

    private class CountingPingHistory : PingHistoryRepository {
        val probes = mutableListOf<PingSource>()
        var error: Throwable? = null

        override fun observeHistory(): Flow<List<PingRecord>> = MutableStateFlow(emptyList())

        override suspend fun refresh() = Unit

        override suspend fun recordProbe(source: PingSource) {
            error?.let { throw it }
            probes += source
        }

        override suspend fun record(result: SpeedTestResult, source: PingSource) = Unit
    }
}
