package com.aura.feature.onboarding.presentation.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.R
import com.aura.feature.home.presentation.format.formatGrouped
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.usecase.BootstrapUseCase
import com.aura.feature.onboarding.domain.usecase.ResolveStartDestinationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_VISIBLE_MILLIS = 1_500L

private const val CHAR_DELAY_MILLIS = 7L

@HiltViewModel
class SplashViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bootstrap: BootstrapUseCase,
    private val resolveStartDestination: ResolveStartDestinationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SplashUiState(log = buildLog(BootConfig.DEFAULT_NODE_COUNT))
    )
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val typing = launch { typeOut() }

            val config = bootstrap()
            val destination = resolveStartDestination()

            if (config.nodeCount != BootConfig.DEFAULT_NODE_COUNT) {
                val log = buildLog(config.nodeCount)
                _uiState.update { state ->
                    state.copy(log = log, printedLength = minOf(state.printedLength, log.length))
                }
            }

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_VISIBLE_MILLIS) delay(MIN_VISIBLE_MILLIS - elapsed)

            typing.cancel()
            _uiState.update { it.copy(startDestination = destination) }
        }
    }

    private suspend fun typeOut() {
        while (_uiState.value.printedLength < _uiState.value.log.length) {
            delay(CHAR_DELAY_MILLIS)
            _uiState.update {
                it.copy(printedLength = (it.printedLength + 1).coerceAtMost(it.log.length))
            }
        }
    }

    private fun buildLog(nodeCount: Int): String = listOf(
        context.getString(R.string.splash_line_1),
        context.getString(R.string.splash_line_2),
        context.getString(R.string.splash_line_3, nodeCount.formatGrouped()),
        context.getString(R.string.splash_line_4),
        context.getString(R.string.splash_line_5),
        context.getString(R.string.splash_line_6),
        context.getString(R.string.splash_line_7),
    ).joinToString("\n")
}
