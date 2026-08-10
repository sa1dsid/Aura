package com.aura.feature.onboarding.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthMode
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository
import com.aura.feature.onboarding.domain.usecase.ContinueWithGoogleUseCase
import com.aura.feature.onboarding.domain.usecase.RequestPasswordResetUseCase
import com.aura.feature.onboarding.domain.usecase.SignInUseCase
import com.aura.feature.onboarding.domain.usecase.SignUpUseCase
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
class AuthViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val continueWithGoogle: ContinueWithGoogleUseCase,
    private val requestPasswordReset: RequestPasswordResetUseCase,
    private val flagsRepository: OnboardingFlagsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = eventChannel.receiveAsFlow()

    fun onModeChange(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, message = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, message = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, message = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.submitting) return

        submit {
            when (state.mode) {
                AuthMode.SIGN_IN -> signIn(state.email, state.password)
                AuthMode.SIGN_UP -> signUp(state.email, state.password)
            }
        }
    }

    fun onGoogleClick() {
        if (_uiState.value.submitting) return
        submit { continueWithGoogle() }
    }

    fun onForgotPasswordClick() {
        val state = _uiState.value
        if (state.submitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, message = null) }
            val email = state.email.trim()
            val result = requestPasswordReset(email)
            _uiState.update { current ->
                current.copy(
                    submitting = false,
                    message = result.fold(
                        onSuccess = { AuthMessage.ResetLinkSent(email) },
                        onFailure = { AuthMessage.Failure(it.toFailure()) },
                    ),
                )
            }
        }
    }

    private fun submit(request: suspend () -> Result<AuthSession>) {
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, message = null) }
            val result = request()
            result.fold(
                onSuccess = { session ->
                    val invitePending = session.accountCreated &&
                        !flagsRepository.flags(session.account.id).inviteScreenPassed
                    _uiState.update { it.copy(submitting = false) }
                    eventChannel.send(
                        if (invitePending) AuthEvent.OpenInvite else AuthEvent.OpenHome
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(submitting = false, message = AuthMessage.Failure(error.toFailure()))
                    }
                },
            )
        }
    }
}

private fun Throwable.toFailure(): AuthFailure =
    (this as? AuthException)?.failure ?: AuthFailure.NETWORK
