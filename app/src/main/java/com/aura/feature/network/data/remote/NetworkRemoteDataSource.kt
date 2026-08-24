package com.aura.feature.network.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.NetworkStateUpdateDto
import com.aura.core.api.dto.PingCreateDto
import com.aura.core.api.dto.PingDto
import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkType
import com.aura.feature.network.data.remote.dto.NetworkSnapshotDto
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestResult
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkRemoteDataSource {
    suspend fun syncState(): NetworkSnapshotDto

    suspend fun summary(): NetworkSnapshotDto

    suspend fun measurements(): List<PingDto>

    suspend fun addPing(pingMs: Int, source: PingSource): PingDto

    suspend fun addSpeedTest(result: SpeedTestResult, source: PingSource): PingDto
}

@Singleton
class ApiNetworkRemoteDataSource @Inject constructor(
    private val api: AuraApi,
    private val networkMonitor: NetworkMonitor,
) : NetworkRemoteDataSource {

    @Volatile
    private var lastProtocol: String? = null

    override suspend fun syncState(): NetworkSnapshotDto {
        val status = networkMonitor.current()
        val state = api.updateNetworkState(
            NetworkStateUpdateDto(
                operator = status.operator,
                connection = status.type.wireName(),
                protocol = lastProtocol,
                vpn = status.isVpnActive,
            )
        )

        lastProtocol = state.ip.protocolName()

        return NetworkSnapshotDto(
            networkType = status.type.name,
            operator = state.operator ?: status.operator,
            ipAddress = state.ip,
            protocol = lastProtocol,
            location = state.location,
            lastTestedAt = null,
            pingMs = null,
            jitterMs = null,
            packetLossPercent = null,
        )
    }

    override suspend fun summary(): NetworkSnapshotDto {
        val status = networkMonitor.current()
        val summary = api.networkSummary()
        lastProtocol = summary.ip.protocolName() ?: summary.protocol

        return NetworkSnapshotDto(
            networkType = status.type.name,
            operator = summary.operator ?: status.operator,
            ipAddress = summary.ip,
            protocol = lastProtocol,
            location = summary.location,
            lastTestedAt = summary.lastTestedAt,
            pingMs = summary.pingMs,
            jitterMs = summary.jitterMs,
            packetLossPercent = summary.packetLossPct,
        )
    }

    override suspend fun measurements(): List<PingDto> = api.measurements()

    override suspend fun addPing(pingMs: Int, source: PingSource): PingDto {
        val status = networkMonitor.current()

        return api.addMeasurement(
            PingCreateDto(
                source = source.wireName,
                operator = status.operator,
                connection = status.type.wireName(),
                protocol = lastProtocol,
                vpn = status.isVpnActive,
                pingMs = pingMs.toDouble(),
            )
        )
    }

    override suspend fun addSpeedTest(result: SpeedTestResult, source: PingSource): PingDto {
        val status = networkMonitor.current()

        return api.addMeasurement(
            PingCreateDto(
                source = source.wireName,
                operator = status.operator,
                connection = status.type.wireName(),
                protocol = lastProtocol,
                vpn = status.isVpnActive,
                pingMs = result.pingMs.toDouble(),
                jitterMs = result.jitterMs.toDouble(),
                packetLossPct = result.packetLossPercent,
                downloadMbps = result.downloadMbps,
                uploadMbps = result.uploadMbps,
            )
        )
    }

    private fun NetworkType.wireName(): String? =
        if (this == NetworkType.NONE) null else name

    private fun String?.protocolName(): String? = when {
        this == null -> null
        contains(':') -> PROTOCOL_IPV6
        else -> PROTOCOL_IPV4
    }

    private companion object {
        const val PROTOCOL_IPV4 = "IPv4"
        const val PROTOCOL_IPV6 = "IPv6"
    }
}
