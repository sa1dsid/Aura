package com.aura.feature.network.domain.repository

import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.PingSource
import com.aura.feature.network.domain.model.SpeedTestResult
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun observeConnection(): Flow<ConnectionDetails>

    fun observeMetrics(): Flow<NetworkMetrics>

    suspend fun refresh()
}

interface PingHistoryRepository {
    fun observeHistory(): Flow<List<PingRecord>>

    suspend fun refresh()

    suspend fun recordProbe(source: PingSource)

    suspend fun record(result: SpeedTestResult, source: PingSource)
}
