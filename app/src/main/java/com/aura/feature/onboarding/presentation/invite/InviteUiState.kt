package com.aura.feature.onboarding.presentation.invite

import com.aura.feature.onboarding.domain.model.InviteFailure

data class InviteUiState(
    val code: String = "",
    val locked: Boolean = false,
    val submitting: Boolean = false,
    val failure: InviteFailure? = null,
)

sealed interface InviteEvent {
    data class Finished(val bonusPopupPending: Boolean) : InviteEvent
}
