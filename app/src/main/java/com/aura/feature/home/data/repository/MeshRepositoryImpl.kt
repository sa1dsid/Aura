package com.aura.feature.home.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.core.network.NetworkMonitor
import com.aura.feature.home.data.mapper.toDomain
import com.aura.feature.home.data.remote.MeshRemoteDataSource
import com.aura.feature.home.domain.model.MeshCity
import com.aura.feature.home.domain.model.MeshState
import com.aura.feature.home.domain.model.NodesOnline
import com.aura.feature.home.domain.model.UserPresence
import com.aura.feature.home.domain.repository.MeshRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
class MeshRepositoryImpl @Inject constructor(
    private val remote: MeshRemoteDataSource,
    private val networkMonitor: NetworkMonitor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MeshRepository {

    private val cities = MutableStateFlow<List<MeshCity>>(emptyList())
    private val nodesOnline = MutableStateFlow<NodesOnline>(NodesOnline.Unknown)
    private val honestPresence = MutableStateFlow<UserPresence?>(null)

    private val refreshMutex = Mutex()
    private var lastFetchAtMillis: Long = 0L

    override fun observeMesh(): Flow<MeshState> =
        combine(
            cities,
            nodesOnline,
            honestPresence,
            networkMonitor.status,
        ) { cityList, nodes, presence, network ->
            MeshState(
                cities = cityList,
                nodesOnline = nodes,
                userPresence = presence?.copy(isPinnedByVpn = network.isVpnActive),
            )
        }

    override suspend fun refresh(force: Boolean) {
        withContext(ioDispatcher) {
            refreshMutex.withLock {
                if (force || isCacheStale()) {
                    fetchSnapshot()
                }
                fetchUserLocation()
            }
        }
    }

    private suspend fun fetchSnapshot() {
        try {
            val snapshot = remote.fetchMeshSnapshot()
            cities.value = snapshot.cities.map { it.toDomain() }
            nodesOnline.value = NodesOnline.Live(snapshot.nodesOnline)
            lastFetchAtMillis = System.currentTimeMillis()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            nodesOnline.value = nodesOnline.value.degradeToLastKnown()
        }
    }

    private suspend fun fetchUserLocation() {
        try {
            val location = remote.fetchUserLocation()
            if (location.vpnActive || networkMonitor.current().isVpnActive) return
            honestPresence.value = location.toDomain()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
        }
    }

    private fun isCacheStale(): Boolean =
        cities.value.isEmpty() ||
            System.currentTimeMillis() - lastFetchAtMillis >= CACHE_TTL_MILLIS

    private fun NodesOnline.degradeToLastKnown(): NodesOnline = when (this) {
        is NodesOnline.Live -> NodesOnline.LastKnown(count)
        is NodesOnline.LastKnown -> this
        NodesOnline.Unknown -> NodesOnline.Unknown
    }

    private companion object {
        val CACHE_TTL_MILLIS = 3.hours.inWholeMilliseconds
    }
}
