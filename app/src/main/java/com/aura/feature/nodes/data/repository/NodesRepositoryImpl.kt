package com.aura.feature.nodes.data.repository

import com.aura.core.api.dto.InviteStateDto
import com.aura.core.api.dto.NodesDto
import com.aura.core.common.IoDispatcher
import com.aura.core.common.runCatchingCancellable
import com.aura.core.config.AppConfigRepository
import com.aura.core.session.SessionCache
import com.aura.feature.nodes.data.mapper.toDomain
import com.aura.feature.nodes.data.remote.NodesRemoteDataSource
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.repository.NodesRepository
import com.aura.feature.onboarding.data.local.SessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private data class NodesSnapshot(
    val nodes: NodesDto,
    val invite: InviteStateDto?,
)

@Singleton
class NodesRepositoryImpl @Inject constructor(
    private val remote: NodesRemoteDataSource,
    private val sessionStore: SessionStore,
    private val appConfigRepository: AppConfigRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NodesRepository, SessionCache {

    private val snapshot = MutableStateFlow<NodesSnapshot?>(null)

    override suspend fun clearSession() {
        snapshot.value = null
    }

    override fun observeNodes(): Flow<NodesState> = combine(
        snapshot.filterNotNull(),
        sessionStore.account,
        appConfigRepository.config,
    ) { snapshot, account, config ->
        snapshot.nodes.toDomain(account = account, invite = snapshot.invite, config = config)
    }

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            appConfigRepository.refresh()

            val nodes = runCatchingCancellable { remote.nodes() }.getOrNull()
                ?: return@withContext

            snapshot.value = NodesSnapshot(
                nodes = nodes,
                invite = runCatchingCancellable { remote.inviteState() }.getOrNull(),
            )
        }
    }
}
