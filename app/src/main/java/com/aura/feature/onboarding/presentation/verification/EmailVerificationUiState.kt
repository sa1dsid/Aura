package com.aura.feature.onboarding.presentation.verification

import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure

data class EmailVerificationUiState(
    val verification: EmailVerification = EmailVerification(""),
    val code: String = "",
    val submitting: Boolean = false,
    val failure: EmailVerificationFailure? = null,
)

sealed interface EmailVerificationEvent {
    data class Confirmed(val invitePending: Boolean) : EmailVerificationEvent

    data object Cancelled : EmailVerificationEvent

    data object CodeSent : EmailVerificationEvent
}
