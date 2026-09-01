package com.aura.feature.home.domain.repository

import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.MeshState
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeHome(): Flow<HomeState>

    suspend fun refresh()

    suspend fun declineBatteryOptimization()

    suspend fun confirmBatteryOptimizationDisabled()

    suspend fun refreshBatteryOptimization()

    suspend fun sendHeartbeat()

    suspend fun markBonusTeaserSeen()

    suspend fun markBonusCongratulationSeen()
}

interface MeshRepository {
    fun observeMesh(): Flow<MeshState>

    suspend fun refresh(force: Boolean = false)
}
