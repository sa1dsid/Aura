package com.aura.feature.nodes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.nodes.domain.usecase.ObserveNodesStateUseCase
import com.aura.feature.nodes.domain.usecase.RefreshNodesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class NodesViewModel @Inject constructor(
    observeNodes: ObserveNodesStateUseCase,
    private val refreshNodes: RefreshNodesUseCase,
) : ViewModel() {

    val uiState: StateFlow<NodesUiState> = observeNodes()
        .map<_, NodesUiState>(NodesUiState::Content)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = NodesUiState.Loading,
        )

    init {
        viewModelScope.launch { refreshNodes() }
    }

    fun onScreenResumed() {
        viewModelScope.launch { refreshNodes() }
    }
}
