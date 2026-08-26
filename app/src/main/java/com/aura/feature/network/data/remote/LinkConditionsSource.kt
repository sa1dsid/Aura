package com.aura.feature.network.data.remote

import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkStatus
import com.aura.core.session.SessionCache
import com.aura.feature.network.domain.model.IpProtocol
import com.aura.feature.network.domain.model.LinkConditions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkConditionsSource @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) : SessionCache {

    private val protocol = MutableStateFlow<IpProtocol?>(null)

    val conditions: Flow<LinkConditions> =
        combine(networkMonitor.status, protocol, ::conditionsOf)

    override suspend fun clearSession() {
        protocol.value = null
    }

    fun remember(protocol: IpProtocol?) {
        val known = protocol ?: return
        this.protocol.value = known
    }

    fun current(): LinkConditions = conditionsOf(networkMonitor.current(), protocol.value)

    private fun conditionsOf(status: NetworkStatus, protocol: IpProtocol?) = LinkConditions(
        networkType = status.type,
        operator = status.operator,
        protocol = protocol,
        isVpnActive = status.isVpnActive,
    )
}
