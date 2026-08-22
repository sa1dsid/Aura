package com.aura.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.network.NetworkMonitor
import com.aura.feature.home.data.session.TestSessionEngine
import com.aura.feature.home.domain.model.TestSessionEvent
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.domain.model.testStartRejection
import com.aura.feature.home.domain.usecase.CreditTestRewardUseCase
import com.aura.feature.home.domain.usecase.ObserveHomeStateUseCase
import com.aura.feature.home.domain.usecase.ObserveMeshStateUseCase
import com.aura.feature.home.domain.usecase.RefreshHomeUseCase
import com.aura.feature.news.domain.repository.NewsRepository
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
    private val creditTestReward: CreditTestRewardUseCase,
    private val sessionEngine: TestSessionEngine,
    newsRepository: NewsRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = eventChannel.receiveAsFlow()

    private var screenVisible = false
    private var eventOnReturn: HomeEvent? = null

    val uiState: StateFlow<HomeUiState> =
        combine(
            observeHomeState(),
            observeMeshState(),
            newsRepository.hasUnread,
        ) { home, mesh, hasUnreadNews ->
            HomeUiState.Content(home = home, mesh = mesh, hasUnreadNews = hasUnreadNews)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading,
        )

    init {
        viewModelScope.launch {
            networkMonitor.status.drop(1).collect { refreshHome() }
        }

        viewModelScope.launch {
            networkMonitor.status.collect { status ->
                sessionEngine.onVpnChanged(status.isVpnActive)
                if (!status.isOnline) sessionEngine.interrupt()
            }
        }

        viewModelScope.launch {
            sessionEngine.events.collect(::onSessionEvent)
        }
    }

    fun onScreenResumed() {
        screenVisible = true
        eventOnReturn?.let { eventChannel.trySend(it) }
        eventOnReturn = null

        viewModelScope.launch { refreshHome() }
    }

    fun onScreenLeft() {
        screenVisible = false
        sessionEngine.interrupt()
    }

    fun onMainButtonClick() {
        val state = uiState.value
        if (state !is HomeUiState.Content) return
        if (state.home.session is TestSessionState.Running) return

        val rejection = testStartRejection(
            session = state.home.session,
            isVpnActive = state.home.connection.isVpnActive,
        )

        if (rejection != null) {
            eventChannel.trySend(HomeEvent.TestRejected(rejection))
            return
        }

        sessionEngine.startTest(state.home.connection.rewardIon)
    }

    private suspend fun onSessionEvent(event: TestSessionEvent) {
        when (event) {
            is TestSessionEvent.Completed -> {
                creditTestReward(event.rewardIon)
                announce(HomeEvent.TestCompleted(event.rewardIon))
            }

            TestSessionEvent.Interrupted -> announce(HomeEvent.TestInterrupted)

            TestSessionEvent.CooldownResumed -> announce(HomeEvent.CooldownResumed)
        }
    }

    private suspend fun announce(event: HomeEvent) {
        if (screenVisible) eventChannel.send(event) else eventOnReturn = event
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
