package com.aura.feature.network

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.aura.core.api.RoutingApiServer
import com.aura.core.geo.UserLocationSource
import com.aura.feature.home.MutableNetworkMonitor
import com.aura.feature.network.data.diagnostics.SpeedTestEngine
import com.aura.feature.network.data.local.NetworkLocalStore
import com.aura.feature.network.data.remote.ApiNetworkRemoteDataSource
import com.aura.feature.network.data.repository.NetworkRepositoryImpl
import com.aura.feature.network.data.repository.PingHistoryRepositoryImpl
import com.aura.feature.network.domain.usecase.ObserveConnectionDetailsUseCase
import com.aura.feature.network.domain.usecase.ObserveNetworkMetricsUseCase
import com.aura.feature.network.domain.usecase.ObservePingHistoryUseCase
import com.aura.feature.network.domain.usecase.RefreshNetworkUseCase
import com.aura.feature.network.presentation.NetworkEvent
import com.aura.feature.network.presentation.NetworkUiState
import com.aura.feature.network.presentation.NetworkViewModel
import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList

internal fun networkAccount(handle: String = "syrex") = Account(
    id = "39",
    email = "smoke@auratest.dev",
    handle = handle,
    inviteLink = "https://io-aura.example/i/syrex",
    authProvider = AuthProvider.EMAIL,
)

internal fun unreadNews(): List<NewsItem> = listOf(
    NewsItem(
        id = "1",
        title = "Mesh update",
        body = "The mesh grew by a thousand nodes.",
        publishedAt = 1_787_702_400_000L,
        read = false,
    )
)

internal class NetworkStack(context: Context, news: List<NewsItem> = emptyList()) {

    val server = RoutingApiServer()

    private val ioDispatcher = Dispatchers.Unconfined

    private val stackScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val viewModelStore = ViewModelStore()

    private val tracked = mutableListOf<ViewModel>()

    val sessionStore = SessionStore()

    val newsRepository = FakeNewsRepository(news)

    val networkMonitor = MutableNetworkMonitor()

    val userLocationSource = UserLocationSource()

    val localStore = NetworkLocalStore(context)

    val pingProbe = FakePingProbe()

    val throughputProbe = FakeThroughputProbe()

    init {
        server.always(NetworkPaths.STATE, body = Net.state(), method = NetworkPaths.PUT)
        server.always(NetworkPaths.SUMMARY, body = Net.summary(), method = NetworkPaths.GET)
        server.always(NetworkPaths.MEASUREMENTS, body = "[]", method = NetworkPaths.GET)
        server.always(NetworkPaths.MEASUREMENTS, body = Net.ping(), method = NetworkPaths.POST)
    }

    val remote = ApiNetworkRemoteDataSource(
        api = server.api,
        networkMonitor = networkMonitor,
        userLocationSource = userLocationSource,
    )

    val repository = NetworkRepositoryImpl(
        remote = remote,
        localStore = localStore,
        networkMonitor = networkMonitor,
        ioDispatcher = ioDispatcher,
    )

    val pingHistory = PingHistoryRepositoryImpl(
        localStore = localStore,
        remote = remote,
        pingProbe = pingProbe,
        ioDispatcher = ioDispatcher,
    )

    val speedTestEngine = SpeedTestEngine(
        scope = stackScope,
        networkMonitor = networkMonitor,
        pingProbe = pingProbe,
        throughputProbe = throughputProbe,
        pingHistory = pingHistory,
    )

    val refreshNetwork = RefreshNetworkUseCase(repository, pingHistory)

    fun signIn(handle: String = "syrex") = sessionStore.open(networkAccount(handle))

    fun signOut() = sessionStore.close()

    fun <T> eventsOf(flow: Flow<T>): List<T> {
        val collected = CopyOnWriteArrayList<T>()
        stackScope.launch { flow.collect { collected += it } }
        return collected
    }

    fun networkViewModel(): NetworkViewModel = track(
        "network",
        NetworkViewModel(
            observeConnection = ObserveConnectionDetailsUseCase(repository),
            observeMetrics = ObserveNetworkMetricsUseCase(repository),
            observeHistory = ObservePingHistoryUseCase(pingHistory),
            newsRepository = newsRepository,
            sessionStore = sessionStore,
            refreshNetwork = refreshNetwork,
            speedTestEngine = speedTestEngine,
            networkMonitor = networkMonitor,
        ),
    )

    fun contentOf(viewModel: NetworkViewModel): List<NetworkUiState> = eventsOf(viewModel.uiState)

    fun eventsOf(viewModel: NetworkViewModel): List<NetworkEvent> = eventsOf(viewModel.events)

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
