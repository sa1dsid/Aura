package com.aura.feature.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.aura.core.api.RoutingApiServer
import com.aura.core.config.AppConfigRepository
import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.nodes.data.remote.ApiNodesRemoteDataSource
import com.aura.feature.nodes.data.repository.NodesRepositoryImpl
import com.aura.feature.nodes.domain.usecase.ObserveNodesStateUseCase
import com.aura.feature.nodes.domain.usecase.RefreshNodesUseCase
import com.aura.feature.nodes.presentation.NodesUiState
import com.aura.feature.nodes.presentation.NodesViewModel
import com.aura.feature.onboarding.data.local.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections

internal fun unreadNews(): List<NewsItem> = listOf(
    NewsItem(
        id = "1",
        title = "Mesh update",
        body = "The mesh grew by a thousand nodes.",
        publishedAt = 1_787_702_400_000L,
        read = false,
    )
)

internal class NodesStack(news: List<NewsItem> = emptyList()) {

    val server = RoutingApiServer()

    private val ioDispatcher = Dispatchers.Unconfined

    private val stackScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val viewModelStore = ViewModelStore()

    private val tracked = mutableListOf<ViewModel>()

    val sessionStore = SessionStore()

    val newsRepository = FakeNewsRepository(news)

    init {
        server.always(NodesPaths.CONFIG, body = Nodes.config())
        server.always(NodesPaths.NODES, body = Nodes.snapshot())
        server.always(NodesPaths.INVITE, body = Nodes.invite())
    }

    val appConfigRepository = AppConfigRepository(server.api, ioDispatcher)

    val remote = ApiNodesRemoteDataSource(server.api)

    val repository = NodesRepositoryImpl(
        remote = remote,
        sessionStore = sessionStore,
        appConfigRepository = appConfigRepository,
        ioDispatcher = ioDispatcher,
    )

    fun signIn(handle: String = "syrex", inviteLink: String = Nodes.ACCOUNT_LINK) {
        sessionStore.open(accountOf(handle = handle, inviteLink = inviteLink))
    }

    fun signOut() = sessionStore.close()

    fun <T> eventsOf(flow: Flow<T>): List<T> {
        val collected = Collections.synchronizedList(mutableListOf<T>())
        stackScope.launch { flow.collect { collected += it } }
        return collected
    }

    fun nodesViewModel(): NodesViewModel = track(
        "nodes",
        NodesViewModel(
            observeNodes = ObserveNodesStateUseCase(repository),
            newsRepository = newsRepository,
            refreshNodes = RefreshNodesUseCase(repository),
        ),
    )

    fun contentOf(viewModel: NodesViewModel): List<NodesUiState> = eventsOf(viewModel.uiState)

    suspend fun close() {
        withTimeoutOrNull(SHUTDOWN_MILLIS) {
            tracked.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
            stackScope.coroutineContext.job.cancelAndJoin()
        }
        viewModelStore.clear()
        server.shutdown()
    }

    private fun <T : ViewModel> track(key: String, viewModel: T): T {
        viewModelStore.put(key, viewModel)
        tracked += viewModel
        return viewModel
    }

    private companion object {
        const val SHUTDOWN_MILLIS = 5_000L
    }
}
