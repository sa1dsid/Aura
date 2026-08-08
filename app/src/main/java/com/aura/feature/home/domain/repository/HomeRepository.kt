package com.aura.feature.home.domain.repository

import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.MeshState
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeHome(): Flow<HomeState>
    suspend fun refresh()
}

interface MeshRepository {
    fun observeMesh(): Flow<MeshState>

    suspend fun refresh(force: Boolean = false)
}
