package com.aura.feature.network.data.repository

import com.aura.core.api.dto.PingDto
import com.aura.core.common.IoDispatcher
import com.aura.feature.network.data.diagnostics.PingProbe
import com.aura.feature.network.data.local.MeasuredQuality
import com.aura.feature.network.data.local.NetworkLocalStore
import com.aura.feature.network.data.mapper.toDomain
import com.aura.feature.network.data.remote.NetworkRemoteDataSource
import com.aura.feature.network.domain.model.PING_HISTORY_LIMIT
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.PingSource
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
    private val pingProbe: PingProbe,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PingHistoryRepository {

    override fun observeHistory(): Flow<List<PingRecord>> = localStore.history

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            val records = try {
                remote.measurements()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                return@withContext
            }

            localStore.replaceAll(records.toRecords())
        }
    }

    override suspend fun recordProbe(source: PingSource) {
        withContext(ioDispatcher) {
            val pingMs = pingProbe.measure() ?: return@withContext
            publish(source) { remote.addPing(pingMs, source) }
        }
    }

    override suspend fun record(result: SpeedTestResult, source: PingSource) {
        withContext(ioDispatcher) {
            localStore.saveQuality(
                MeasuredQuality(
                    jitterMs = result.jitterMs,
                    packetLossPercent = result.packetLossPercent,
                )
            )
            publish(source) { remote.addSpeedTest(result, source) }
        }
    }

    private suspend fun publish(source: PingSource, send: suspend () -> PingDto) {
        val stored = try {
            send()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            null
        }

        val record = stored?.toDomain()
        if (record != null) {
            localStore.append(record)
            return
        }

        if (source != PingSource.BACKGROUND) refresh()
    }

    private fun List<PingDto>.toRecords(): List<PingRecord> = mapNotNull(PingDto::toDomain)
        .sortedBy(PingRecord::timestamp)
        .takeLast(PING_HISTORY_LIMIT)
}
