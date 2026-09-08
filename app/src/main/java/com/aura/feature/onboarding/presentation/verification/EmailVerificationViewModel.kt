package com.aura.feature.onboarding.presentation.verification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.common.TimeSource
import com.aura.feature.onboarding.domain.model.EMAIL_CODE_RESEND_COOLDOWN
import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.model.EmailVerificationException
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.domain.model.isWholeEmailCode
import com.aura.feature.onboarding.domain.model.toEmailCode
import com.aura.feature.onboarding.domain.usecase.ConfirmEmailUseCase
import com.aura.feature.onboarding.domain.usecase.ResendEmailCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val KEY_EMAIL = "verification_email"

private const val KEY_CODE = "verification_code"

private const val KEY_COOLDOWN_UNTIL = "verification_cooldown_until"

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val confirmEmail: ConfirmEmailUseCase,
    private val resendEmailCode: ResendEmailCodeUseCase,
    private val timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<EmailVerificationEvent>(Channel.BUFFERED)
    val events: Flow<EmailVerificationEvent> = eventChannel.receiveAsFlow()

    private var cooldownJob: Job? = null

    fun onScreenOpened(verification: EmailVerification) {
        if (_uiState.value.verification.email == verification.email) return

        if (savedStateHandle.get<String>(KEY_EMAIL) != verification.email) {
            savedStateHandle[KEY_EMAIL] = verification.email
            savedStateHandle[KEY_CODE] = ""
            savedStateHandle.remove<Long>(KEY_COOLDOWN_UNTIL)
        }

        _uiState.value = EmailVerificationUiState(
            verification = verification,
            code = savedStateHandle[KEY_CODE] ?: "",
        )

        val until = savedStateHandle.get<Long>(KEY_COOLDOWN_UNTIL)
        when {
            until != null -> {
                val left = (until - timeSource.nowMillis()).milliseconds
                if (left > Duration.ZERO) startResendCooldown(left)
            }

            verification.codeJustSent -> startResendCooldown()
        }
    }

    fun onCodeChange(code: String) {
        val normalized = code.toEmailCode()
        savedStateHandle[KEY_CODE] = normalized
        _uiState.update { it.copy(code = normalized, failure = null) }
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
        if (!_uiState.value.canResend) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, failure = null) }

            resendEmailCode(_uiState.value.verification.email).fold(
                onSuccess = {
                    savedStateHandle[KEY_CODE] = ""
                    _uiState.update { it.copy(code = "", submitting = false) }
                    startResendCooldown()
                    eventChannel.send(EmailVerificationEvent.CodeSent)
                },
                onFailure = { error ->
                    report(error)
                    if (_uiState.value.failure == EmailVerificationFailure.RESEND_TOO_SOON) {
                        startResendCooldown()
                    }
                },
            )
        }
    }

    fun onCancel() {
        viewModelScope.launch { eventChannel.send(EmailVerificationEvent.Cancelled) }
    }

    private fun startResendCooldown(cooldown: Duration = EMAIL_CODE_RESEND_COOLDOWN) {
        cooldownJob?.cancel()
        savedStateHandle[KEY_COOLDOWN_UNTIL] = timeSource.nowMillis() + cooldown.inWholeMilliseconds
        cooldownJob = viewModelScope.launch {
            var left = cooldown
            while (left > Duration.ZERO) {
                _uiState.update { it.copy(resendCooldown = left) }
                delay(COOLDOWN_STEP)
                left -= COOLDOWN_STEP
            }
            _uiState.update { it.copy(resendCooldown = Duration.ZERO) }
        }
    }

    private fun report(error: Throwable) {
        _uiState.update { it.copy(submitting = false, failure = error.toFailure()) }
    }
}

private val COOLDOWN_STEP = 1.seconds

private fun Throwable.toFailure(): EmailVerificationFailure =
    (this as? EmailVerificationException)?.failure ?: EmailVerificationFailure.NETWORK
