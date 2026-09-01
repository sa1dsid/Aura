package com.aura.feature.onboarding.presentation.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.model.EmailVerificationException
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.domain.model.isWholeEmailCode
import com.aura.feature.onboarding.domain.model.toEmailCode
import com.aura.feature.onboarding.domain.usecase.ConfirmEmailUseCase
import com.aura.feature.onboarding.domain.usecase.ResendEmailCodeUseCase
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
class EmailVerificationViewModel @Inject constructor(
    private val confirmEmail: ConfirmEmailUseCase,
    private val resendEmailCode: ResendEmailCodeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<EmailVerificationEvent>(Channel.BUFFERED)
    val events: Flow<EmailVerificationEvent> = eventChannel.receiveAsFlow()

    fun onScreenOpened(verification: EmailVerification) {
        if (_uiState.value.verification.email == verification.email) return
        _uiState.value = EmailVerificationUiState(verification = verification)
    }

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(code = code.toEmailCode(), failure = null) }
    }

    fun onPaste(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) return
        onCodeChange(clipboardText)
    }

    fun onConfirmClick() {
        val state = _uiState.value
        if (state.submitting || !state.code.isWholeEmailCode) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, failure = null) }

            confirmEmail(state.verification.email, state.code).fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(submitting = false) }
                    eventChannel.send(EmailVerificationEvent.Confirmed(session.invitePending))
                },
                onFailure = ::report,
            )
        }
    }

    fun onResendClick() {
        if (_uiState.value.submitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, failure = null) }

            resendEmailCode(_uiState.value.verification.email).fold(
                onSuccess = {
                    _uiState.update { it.copy(code = "", submitting = false) }
                    eventChannel.send(EmailVerificationEvent.CodeSent)
                },
                onFailure = ::report,
            )
        }
    }

    fun onCancel() {
        viewModelScope.launch { eventChannel.send(EmailVerificationEvent.Cancelled) }
    }

    private fun report(error: Throwable) {
        _uiState.update { it.copy(submitting = false, failure = error.toFailure()) }
    }
}

private fun Throwable.toFailure(): EmailVerificationFailure =
    (this as? EmailVerificationException)?.failure ?: EmailVerificationFailure.NETWORK
