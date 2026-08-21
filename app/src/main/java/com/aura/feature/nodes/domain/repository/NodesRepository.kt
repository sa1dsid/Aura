package com.aura.feature.nodes.domain.repository

import com.aura.feature.nodes.domain.model.NodesState
import kotlinx.coroutines.flow.Flow

interface NodesRepository {
    fun observeNodes(): Flow<NodesState>

    suspend fun refresh()
}
