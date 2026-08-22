package com.aura.feature.onboarding.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.auth.GoogleSignInClient
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthMode
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.usecase.ContinueWithGoogleUseCase
import com.aura.feature.onboarding.domain.usecase.RequestPasswordResetUseCase
import com.aura.feature.onboarding.domain.usecase.SignInUseCase
import com.aura.feature.onboarding.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    private val googleSignInClient: GoogleSignInClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch { googleSignInClient.warmUp() }
    }

    fun onModeChange(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, invalidField = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                invalidField = it.invalidField.takeUnless { field -> field == AuthField.EMAIL },
            )
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                invalidField = it.invalidField.takeUnless { field -> field == AuthField.PASSWORD },
            )
        }
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

    fun onGoogleClick(activityContext: Context) {
        if (_uiState.value.submitting) return

        submit {
            try {
                continueWithGoogle(googleSignInClient.idToken(activityContext))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }
    }

    fun onForgotPasswordClick() {
        val state = _uiState.value
        if (state.submitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, invalidField = null) }
            requestPasswordReset(state.email).fold(
                onSuccess = {
                    _uiState.update { it.copy(submitting = false) }
                    eventChannel.send(AuthEvent.ShowToast(AuthToast.RESET_LINK_SENT))
                },
                onFailure = { error -> reportFailure(error.toFailure()) },
            )
        }
    }

    private fun submit(request: suspend () -> Result<AuthSession>) {
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, invalidField = null) }
            request().fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(submitting = false) }
                    eventChannel.send(
                        if (session.invitePending) AuthEvent.OpenInvite else AuthEvent.OpenHome
                    )
                },
                onFailure = { error -> reportFailure(error.toFailure()) },
            )
        }
    }

    private suspend fun reportFailure(failure: AuthFailure) {
        _uiState.update { it.copy(submitting = false, invalidField = failure.toField()) }
        failure.toToast()?.let { eventChannel.send(AuthEvent.ShowToast(it)) }
    }
}

private fun Throwable.toFailure(): AuthFailure =
    (this as? AuthException)?.failure ?: AuthFailure.NETWORK

private fun AuthFailure.toToast(): AuthToast? = when (this) {
    AuthFailure.EMAIL_REQUIRED -> AuthToast.EMAIL_REQUIRED
    AuthFailure.EMAIL_INVALID -> AuthToast.EMAIL_INVALID
    AuthFailure.PASSWORD_TOO_SHORT -> AuthToast.PASSWORD_TOO_SHORT
    AuthFailure.EMAIL_ALREADY_REGISTERED -> AuthToast.ACCOUNT_EXISTS
    AuthFailure.ACCOUNT_NOT_FOUND -> AuthToast.NO_ACCOUNT
    AuthFailure.WRONG_PASSWORD -> AuthToast.WRONG_CREDENTIALS
    AuthFailure.GOOGLE_UNAVAILABLE -> AuthToast.GOOGLE_UNAVAILABLE
    AuthFailure.NETWORK -> AuthToast.NO_CONNECTION
    AuthFailure.GOOGLE_CANCELLED -> null
}

private fun AuthFailure.toField(): AuthField? = when (this) {
    AuthFailure.EMAIL_REQUIRED,
    AuthFailure.EMAIL_INVALID,
    AuthFailure.EMAIL_ALREADY_REGISTERED,
    AuthFailure.ACCOUNT_NOT_FOUND,
    -> AuthField.EMAIL

    AuthFailure.PASSWORD_TOO_SHORT,
    AuthFailure.WRONG_PASSWORD,
    -> AuthField.PASSWORD

    AuthFailure.NETWORK,
    AuthFailure.GOOGLE_UNAVAILABLE,
    AuthFailure.GOOGLE_CANCELLED,
    -> null
}
