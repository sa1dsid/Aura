package com.aura.feature.nodes.domain.usecase

import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.repository.NodesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNodesStateUseCase @Inject constructor(
    private val repository: NodesRepository,
) {
    operator fun invoke(): Flow<NodesState> = repository.observeNodes()
}

class RefreshNodesUseCase @Inject constructor(
    private val repository: NodesRepository,
) {
    suspend operator fun invoke() = repository.refresh()
}
