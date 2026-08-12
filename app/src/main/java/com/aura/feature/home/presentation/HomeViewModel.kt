package com.aura.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.network.NetworkMonitor
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.testStartRejection
import com.aura.feature.home.domain.usecase.ObserveHomeStateUseCase
import com.aura.feature.home.domain.usecase.ObserveMeshStateUseCase
import com.aura.feature.home.domain.usecase.RefreshHomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeState: ObserveHomeStateUseCase,
    observeMeshState: ObserveMeshStateUseCase,
    private val refreshHome: RefreshHomeUseCase,
    private val sessionEngine: TestSessionEngine,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = eventChannel.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> =
        combine(observeHomeState(), observeMeshState()) { home, mesh ->
            HomeUiState.Content(home = home, mesh = mesh)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading,
        )

    init {
        viewModelScope.launch {
            networkMonitor.status.drop(1).collect { refreshHome() }
        }
    }

    fun onScreenResumed() {
        viewModelScope.launch { refreshHome() }
    }

    fun onMainButtonClick() {
        val state = uiState.value
        if (state !is HomeUiState.Content) return
        if (state.home.session is TestSessionState.Running) return

        val rejection = testStartRejection(
            session = state.home.session,
            isVpnActive = state.home.connection.isVpnActive || networkMonitor.current().isVpnActive,
        )

        if (rejection != null) {
            eventChannel.trySend(HomeEvent.TestRejected(rejection))
            return
        }

        sessionEngine.startTest(state.home.connection.rewardIon)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
