package com.aura.feature.network.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.core.common.logFailure
import com.aura.core.common.runCatchingCancellable
import com.aura.core.geo.UserLocationSource
import com.aura.core.session.SessionCache
import com.aura.feature.network.data.local.NetworkLocalStore
import com.aura.feature.network.data.mapper.protocolOf
import com.aura.feature.network.data.mapper.toConnection
import com.aura.feature.network.data.mapper.toMetrics
import com.aura.feature.network.data.remote.LinkConditionsSource
import com.aura.feature.network.data.remote.NetworkRemoteDataSource
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.repository.NetworkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private data class NetworkSnapshot(
    val connection: ConnectionDetails,
    val metrics: NetworkMetrics,
)

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val remote: NetworkRemoteDataSource,
    private val localStore: NetworkLocalStore,
    private val linkConditions: LinkConditionsSource,
    private val userLocationSource: UserLocationSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NetworkRepository, SessionCache {

    private val snapshot = MutableStateFlow<NetworkSnapshot?>(null)

    override suspend fun clearSession() {
        snapshot.value = null
    }

    override fun observeConnection(): Flow<ConnectionDetails> =
        combine(snapshot.filterNotNull(), linkConditions.conditions) { snapshot, conditions ->
            snapshot.connection.copy(
                networkType = conditions.networkType,
                isVpnActive = conditions.isVpnActive,
            )
        }

    override fun observeMetrics(): Flow<NetworkMetrics> =
        combine(snapshot, localStore.measured) { snapshot, measured ->
            NetworkMetrics(
                pingMs = measured.pingMs ?: snapshot?.metrics?.pingMs,
                jitterMs = measured.jitterMs ?: snapshot?.metrics?.jitterMs,
                packetLossPercent = measured.packetLossPercent
                    ?: snapshot?.metrics?.packetLossPercent,
            )
        }

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            val conditions = linkConditions.current()

            val state = runCatchingCancellable { remote.syncState(conditions) }
                .logFailure("network state")
                .getOrNull()
                ?.also { remember(ip = it.ip, protocol = it.protocol, location = it.location) }

            val summary = runCatchingCancellable { remote.summary() }
                .logFailure("network summary")
                .getOrNull()
                ?.also { remember(ip = it.ip, protocol = it.protocol, location = it.location) }

            val fresh = summary?.let {
                NetworkSnapshot(it.toConnection(conditions), it.toMetrics())
            } ?: state?.let {
                NetworkSnapshot(it.toConnection(conditions), NetworkMetrics.Empty)
            }

            if (fresh != null) snapshot.value = fresh
        }
    }

    private fun remember(ip: String?, protocol: String?, location: String?) {
        linkConditions.remember(protocolOf(ip, protocol))
        userLocationSource.remember(location)
    }
}
