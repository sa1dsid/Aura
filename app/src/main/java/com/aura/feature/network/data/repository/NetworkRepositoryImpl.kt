package com.aura.feature.network.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.core.network.NetworkMonitor
import com.aura.feature.network.data.local.NetworkLocalStore
import com.aura.feature.network.data.mapper.toDomain
import com.aura.feature.network.data.remote.NetworkRemoteDataSource
import com.aura.feature.network.data.remote.dto.NetworkSnapshotDto
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.repository.NetworkRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val remote: NetworkRemoteDataSource,
    private val localStore: NetworkLocalStore,
    private val networkMonitor: NetworkMonitor,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NetworkRepository {

    private val snapshot = MutableStateFlow<NetworkSnapshotDto?>(null)

    override fun observeConnection(): Flow<ConnectionDetails> =
        combine(snapshot.filterNotNull(), networkMonitor.status) { dto, status ->
            dto.toDomain(isVpnActive = status.isVpnActive)
        }

    override fun observeMetrics(): Flow<NetworkMetrics> =
        combine(localStore.history, localStore.quality) { history, quality ->
            NetworkMetrics(
                pingMs = history.lastOrNull()?.pingMs,
                jitterMs = quality?.jitterMs,
                packetLossPercent = quality?.packetLossPercent,
            )
        }

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            try {
                snapshot.value = remote.fetchSnapshot()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
            }
        }
    }
}
