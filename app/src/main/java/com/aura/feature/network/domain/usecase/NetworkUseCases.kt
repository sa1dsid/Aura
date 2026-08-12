package com.aura.feature.network.domain.usecase

import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.repository.NetworkRepository
import com.aura.feature.network.domain.repository.PingHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionDetailsUseCase @Inject constructor(
    private val repository: NetworkRepository,
) {
    operator fun invoke(): Flow<ConnectionDetails> = repository.observeConnection()
}

class ObserveNetworkMetricsUseCase @Inject constructor(
    private val repository: NetworkRepository,
) {
    operator fun invoke(): Flow<NetworkMetrics> = repository.observeMetrics()
}

class ObservePingHistoryUseCase @Inject constructor(
    private val repository: PingHistoryRepository,
) {
    operator fun invoke(): Flow<List<PingRecord>> = repository.observeHistory()
}

class RefreshNetworkUseCase @Inject constructor(
    private val repository: NetworkRepository,
) {
    suspend operator fun invoke() = repository.refresh()
}
