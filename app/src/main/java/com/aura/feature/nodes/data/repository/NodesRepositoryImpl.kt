package com.aura.feature.nodes.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.feature.nodes.data.mapper.toDomain
import com.aura.feature.nodes.data.remote.NodesRemoteDataSource
import com.aura.feature.nodes.data.remote.dto.NodesSnapshotDto
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.repository.NodesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodesRepositoryImpl @Inject constructor(
    private val remote: NodesRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NodesRepository {

    private val snapshot = MutableStateFlow<NodesSnapshotDto?>(null)

    override fun observeNodes(): Flow<NodesState> =
        snapshot.filterNotNull().map(NodesSnapshotDto::toDomain)

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            try {
                snapshot.value = remote.fetchNodes()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
            }
        }
    }
}
