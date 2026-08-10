package com.aura.feature.onboarding.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository
import com.aura.feature.onboarding.domain.usecase.ApplyInviteCodeUseCase
import com.aura.feature.onboarding.domain.usecase.ObserveInviteAttributionUseCase
import com.aura.feature.onboarding.domain.usecase.SkipInviteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val flagsRepository: OnboardingFlagsRepository,
    private val observeInviteAttribution: ObserveInviteAttributionUseCase,
    private val applyInviteCode: ApplyInviteCodeUseCase,
    private val skipInvite: SkipInviteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteUiState())
    val uiState: StateFlow<InviteUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<InviteEvent>(Channel.BUFFERED)
    val events: Flow<InviteEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val attribution = observeInviteAttribution()) {
                is InviteAttribution.FromLink ->
                    _uiState.update { it.copy(code = attribution.code, locked = true) }

                InviteAttribution.None -> Unit
            }
        }
    }

    fun onCodeChange(code: String) {
        if (_uiState.value.locked) return
        _uiState.update { it.copy(code = code, failure = null) }
    }

    fun onPaste(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) return
        onCodeChange(
            clipboardText.trim().uppercase().filter { it.isLetterOrDigit() }
        )
    }

    fun onApplyClick() {
        val state = _uiState.value
        if (state.submitting || state.code.isBlank()) return

        finish { accountId -> applyInviteCode(accountId, state.code) }
    }

    fun onSkipClick() {
        if (_uiState.value.submitting) return

        finish { accountId -> skipInvite(accountId) }
    }

    private fun finish(request: suspend (String) -> Result<Unit>) {
        viewModelScope.launch {
            val accountId = authRepository.currentAccount()?.id ?: return@launch
            _uiState.update { it.copy(submitting = true, failure = null) }

            request(accountId).fold(
                onSuccess = {
                    val flags = flagsRepository.flags(accountId)
                    _uiState.update { it.copy(submitting = false) }
                    eventChannel.send(InviteEvent.Finished(!flags.bonusPopupShown))
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            failure = (error as? InviteException)?.failure ?: InviteFailure.NETWORK,
                        )
                    }
                },
            )
        }
    }
}
