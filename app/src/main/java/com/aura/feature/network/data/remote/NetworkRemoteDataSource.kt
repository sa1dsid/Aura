package com.aura.feature.network.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.NetworkStateDto
import com.aura.core.api.dto.NetworkStateUpdateDto
import com.aura.core.api.dto.NetworkSummaryDto
import com.aura.core.api.dto.PingCreateDto
import com.aura.core.api.dto.PingDto
import com.aura.feature.network.data.mapper.toWire
import com.aura.feature.network.domain.model.LinkConditions
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestResult
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkRemoteDataSource {

    suspend fun syncState(conditions: LinkConditions): NetworkStateDto

    suspend fun summary(): NetworkSummaryDto

    suspend fun measurements(): List<PingDto>

    suspend fun addPing(conditions: LinkConditions, source: PingSource, pingMs: Int): PingDto

    suspend fun addSpeedTest(
        conditions: LinkConditions,
        source: PingSource,
        result: SpeedTestResult,
    ): PingDto
}

@Singleton
class ApiNetworkRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : NetworkRemoteDataSource {

    override suspend fun syncState(conditions: LinkConditions): NetworkStateDto =
        api.updateNetworkState(
            NetworkStateUpdateDto(
                operator = conditions.operator,
                connection = conditions.networkType.toWire(),
                protocol = conditions.protocol?.toWire(),
                vpn = conditions.isVpnActive,
            )
        )

    override suspend fun summary(): NetworkSummaryDto = api.networkSummary()

    override suspend fun measurements(): List<PingDto> = api.measurements()

    override suspend fun addPing(
        conditions: LinkConditions,
        source: PingSource,
        pingMs: Int,
    ): PingDto = api.addMeasurement(conditions.measurement(source, pingMs.toDouble()))

    override suspend fun addSpeedTest(
        conditions: LinkConditions,
        source: PingSource,
        result: SpeedTestResult,
    ): PingDto = api.addMeasurement(
        conditions.measurement(source, result.pingMs.toDouble()).copy(
            jitterMs = result.jitterMs.toDouble(),
            packetLossPct = result.packetLossPercent,
            downloadMbps = result.downloadMbps,
            uploadMbps = result.uploadMbps,
        )
    )

    private fun LinkConditions.measurement(source: PingSource, pingMs: Double) = PingCreateDto(
        source = source.wireName,
        operator = operator,
        connection = networkType.toWire(),
        protocol = protocol?.toWire(),
        vpn = isVpnActive,
        pingMs = pingMs,
    )
}
