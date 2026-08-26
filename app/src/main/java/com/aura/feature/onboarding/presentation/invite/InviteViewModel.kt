package com.aura.feature.onboarding.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import com.aura.feature.onboarding.domain.model.isWholeInviteCode
import com.aura.feature.onboarding.domain.model.toInviteCode
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository
import com.aura.feature.onboarding.domain.usecase.ApplyInviteCodeUseCase
import com.aura.feature.onboarding.domain.usecase.ObserveInviteAttributionUseCase
import com.aura.feature.onboarding.domain.usecase.SkipInviteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    private var loadJob: Job? = null

    private var preparedAccountId: String? = null

    fun onScreenResumed() {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            val accountId = signedInAccountId() ?: return@launch
            if (accountId == preparedAccountId) return@launch
            preparedAccountId = accountId

            _uiState.value = when (val attribution = observeInviteAttribution()) {
                is InviteAttribution.FromLink ->
                    InviteUiState(code = attribution.code, locked = true)

                InviteAttribution.None -> InviteUiState()
            }
        }
    }

    fun onCodeChange(code: String) {
        if (_uiState.value.locked) return
        _uiState.update { it.copy(code = code.toInviteCode(), failure = null) }
    }

    fun onPaste(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) return
        onCodeChange(clipboardText)
    }

    fun onApplyClick() {
        val state = _uiState.value
        if (state.submitting || !state.code.isWholeInviteCode) return

        settle { applyInviteCode(state.code) }
    }

    fun onSkipClick() {
        if (_uiState.value.submitting) return

        settle { skipInvite() }
    }

    private fun settle(decision: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            signedInAccountId() ?: return@launch
            _uiState.update { it.copy(submitting = true, failure = null) }

            decision().fold(
                onSuccess = {
                    val bonusPopupPending = !flagsRepository.flags().bonusPopupShown
                    _uiState.update { it.copy(submitting = false) }
                    eventChannel.send(InviteEvent.Finished(bonusPopupPending))
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(submitting = false, failure = error.toFailure())
                    }
                },
            )
        }
    }

    private suspend fun signedInAccountId(): String? {
        val accountId = authRepository.currentAccount()?.id
        if (accountId == null) eventChannel.send(InviteEvent.SessionLost)
        return accountId
    }
}

private fun Throwable.toFailure(): InviteFailure =
    (this as? InviteException)?.failure ?: InviteFailure.NETWORK
