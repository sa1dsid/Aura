package com.aura.feature.network.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.network.NetworkMonitor
import com.aura.feature.network.data.diagnostics.SpeedTestEngine
import com.aura.feature.news.domain.repository.NewsRepository
import com.aura.feature.network.domain.model.SpeedTestState
import com.aura.feature.network.domain.usecase.ObserveConnectionDetailsUseCase
import com.aura.feature.network.domain.usecase.ObserveNetworkMetricsUseCase
import com.aura.feature.network.domain.usecase.ObservePingHistoryUseCase
import com.aura.feature.network.domain.usecase.RefreshNetworkUseCase
import com.aura.feature.onboarding.data.local.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    observeConnection: ObserveConnectionDetailsUseCase,
    observeMetrics: ObserveNetworkMetricsUseCase,
    observeHistory: ObservePingHistoryUseCase,
    newsRepository: NewsRepository,
    sessionStore: SessionStore,
    private val refreshNetwork: RefreshNetworkUseCase,
    private val speedTestEngine: SpeedTestEngine,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _events = MutableSharedFlow<NetworkEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<NetworkEvent> = _events.asSharedFlow()

    val uiState: StateFlow<NetworkUiState> = combine(
        observeConnection(),
        observeMetrics(),
        observeHistory(),
        speedTestEngine.state,
        sessionStore.account,
    ) { connection, metrics, history, diagnostics, account ->
        NetworkUiState.Content(
            handle = account?.handle,
            connection = connection,
            metrics = metrics,
            history = history,
            diagnostics = diagnostics,
            hasUnreadNews = false,
        )
    }.combine(newsRepository.hasUnread) { content, hasUnreadNews ->
        content.copy(hasUnreadNews = hasUnreadNews)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = NetworkUiState.Loading,
    )

    init {
        viewModelScope.launch {
            networkMonitor.status.drop(1).collect { refreshNetwork() }
        }
        viewModelScope.launch {
            speedTestEngine.failures.collect { _events.tryEmit(NetworkEvent.DiagnosticsFailed(it)) }
        }
    }

    fun onScreenResumed() {
        viewModelScope.launch { refreshNetwork() }
    }

    fun onStartTestClick() {
        speedTestEngine.start()
    }

    fun onShareResultClick() {
        val diagnostics = speedTestEngine.state.value
        if (diagnostics !is SpeedTestState.Done) return
        _events.tryEmit(NetworkEvent.ShareResult(diagnostics.result))
    }

    fun onVpnCardClick() {
        _events.tryEmit(NetworkEvent.OpenVpnSettings)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
