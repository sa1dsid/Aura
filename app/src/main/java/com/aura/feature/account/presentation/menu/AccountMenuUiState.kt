package com.aura.feature.account.presentation.menu

import androidx.compose.runtime.Immutable
import com.aura.feature.account.domain.model.AccountProfile

@Immutable
data class AccountMenuUiState(
    val profile: AccountProfile? = null,
    val pushNotifications: Boolean = true,
    val isDeleteConfirmVisible: Boolean = false,
)

sealed interface AccountMenuEvent {

    data class OpenUrl(val url: String) : AccountMenuEvent

    data object SessionClosed : AccountMenuEvent
}

@Immutable
data class AccountMenuActions(
    val onPushNotificationsChange: (Boolean) -> Unit = {},
    val onTermsClick: () -> Unit = {},
    val onPrivacyClick: () -> Unit = {},
    val onLogOutClick: () -> Unit = {},
    val onDeleteAccountClick: () -> Unit = {},
    val onKeepAccountClick: () -> Unit = {},
    val onDeletePermanentlyClick: () -> Unit = {},
    val onDismissRequest: () -> Unit = {},
)
