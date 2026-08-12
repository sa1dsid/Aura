package com.aura.feature.network.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.core.network.NetworkMonitor
import com.aura.feature.network.data.diagnostics.PingProbe
import com.aura.feature.network.data.local.MeasuredQuality
import com.aura.feature.network.data.local.NetworkLocalStore
import com.aura.feature.network.data.mapper.locationLabel
import com.aura.feature.network.data.remote.NetworkRemoteDataSource
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PingHistoryRepositoryImpl @Inject constructor(
    private val localStore: NetworkLocalStore,
    private val remote: NetworkRemoteDataSource,
    private val networkMonitor: NetworkMonitor,
    private val pingProbe: PingProbe,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PingHistoryRepository {

    override fun observeHistory(): Flow<List<PingRecord>> = localStore.history

    override suspend fun recordProbe() {
        withContext(ioDispatcher) {
            val pingMs = pingProbe.measure() ?: return@withContext
            append(pingMs)
        }
    }

    override suspend fun record(result: SpeedTestResult) {
        withContext(ioDispatcher) {
            append(result.pingMs)
            localStore.saveQuality(
                MeasuredQuality(
                    jitterMs = result.jitterMs,
                    packetLossPercent = result.packetLossPercent,
                )
            )
        }
    }

    private suspend fun append(pingMs: Int) {
        val snapshot = try {
            remote.fetchSnapshot()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            null
        }

        localStore.append(
            PingRecord(
                timestamp = System.currentTimeMillis(),
                ipAddress = snapshot?.ipAddress,
                operator = snapshot?.operator,
                pingMs = pingMs,
                location = snapshot?.locationLabel(),
                vpnActive = networkMonitor.current().isVpnActive,
            )
        )
    }
}
