package com.aura.feature.onboarding.presentation.auth

import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthMode

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val message: AuthMessage? = null,
)

sealed interface AuthMessage {
    data class Failure(val failure: AuthFailure) : AuthMessage

    data class ResetLinkSent(val email: String) : AuthMessage
}

sealed interface AuthEvent {
    data object OpenHome : AuthEvent

    data object OpenInvite : AuthEvent
}
