package com.aura.feature.network.data.repository

import com.aura.core.api.dto.PingDto
import com.aura.core.common.IoDispatcher
import com.aura.core.common.logFailure
import com.aura.core.common.runCatchingCancellable
import com.aura.feature.network.data.diagnostics.PingProbe
import com.aura.feature.network.data.local.NetworkLocalStore
import com.aura.feature.network.data.mapper.toRecord
import com.aura.feature.network.data.remote.LinkConditionsSource
import com.aura.feature.network.data.remote.NetworkRemoteDataSource
import com.aura.feature.network.domain.model.PING_HISTORY_LIMIT
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PingHistoryRepositoryImpl @Inject constructor(
    private val localStore: NetworkLocalStore,
    private val remote: NetworkRemoteDataSource,
    private val linkConditions: LinkConditionsSource,
    private val pingProbe: PingProbe,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PingHistoryRepository {

    override fun observeHistory(): Flow<List<PingRecord>> = localStore.history

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            val measurements = runCatchingCancellable { remote.measurements() }
                .logFailure("network measurements")
                .getOrNull()
                ?: return@withContext

            localStore.replaceAll(measurements.toRecords())
        }
    }

    override suspend fun recordProbe(source: PingSource) {
        withContext(ioDispatcher) {
            val pingMs = pingProbe.measure() ?: return@withContext
            localStore.savePing(pingMs)
            publish(source) { remote.addPing(linkConditions.current(), source, pingMs) }
        }
    }

    override suspend fun record(result: SpeedTestResult, source: PingSource) {
        withContext(ioDispatcher) {
            localStore.saveSpeedTest(
                pingMs = result.pingMs,
                jitterMs = result.jitterMs,
                packetLossPercent = result.packetLossPercent,
            )
            publish(source) { remote.addSpeedTest(linkConditions.current(), source, result) }
        }
    }

    private suspend fun publish(source: PingSource, send: suspend () -> PingDto) {
        val record = runCatchingCancellable { send() }
            .logFailure("network measurement")
            .getOrNull()
            ?.toRecord()

        if (record != null) {
            localStore.append(record)
            return
        }

        if (source != PingSource.BACKGROUND) refresh()
    }

    private fun List<PingDto>.toRecords(): List<PingRecord> = mapNotNull(PingDto::toRecord)
        .sortedBy(PingRecord::timestamp)
        .takeLast(PING_HISTORY_LIMIT)
}
