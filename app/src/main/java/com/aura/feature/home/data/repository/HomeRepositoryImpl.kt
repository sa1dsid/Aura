package com.aura.feature.home.data.repository

import com.aura.core.common.IoDispatcher
import com.aura.feature.home.data.mapper.toDomain
import com.aura.feature.home.data.remote.HomeRemoteDataSource
import com.aura.feature.home.data.remote.dto.HomeSnapshotDto
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remote: HomeRemoteDataSource,
    private val sessionEngine: TestSessionEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HomeRepository {

    private val snapshot = MutableStateFlow<HomeSnapshotDto?>(null)

    override fun observeHome(): Flow<HomeState> =
        combine(snapshot.filterNotNull(), sessionEngine.state) { dto, session ->
            dto.toDomain(session)
        }

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            try {
                snapshot.value = remote.fetchHome()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
            }
        }
    }
}
