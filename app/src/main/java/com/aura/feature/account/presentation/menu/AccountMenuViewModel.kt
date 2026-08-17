package com.aura.feature.account.presentation.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.account.domain.model.LegalLinks
import com.aura.feature.account.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountMenuViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val pushNotifications = MutableStateFlow(true)
    private val deleteConfirmVisible = MutableStateFlow(false)

    private val _events = MutableSharedFlow<AccountMenuEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AccountMenuEvent> = _events.asSharedFlow()

    val uiState: StateFlow<AccountMenuUiState> = combine(
        accountRepository.profile,
        pushNotifications,
        deleteConfirmVisible,
    ) { profile, pushEnabled, deleteVisible ->
        AccountMenuUiState(
            profile = profile,
            pushNotifications = pushEnabled,
            isDeleteConfirmVisible = deleteVisible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AccountMenuUiState(),
    )

    fun onMenuOpened() {
        viewModelScope.launch {
            accountRepository.pushNotifications().onSuccess { pushNotifications.value = it }
        }
    }

    fun onPushNotificationsChange(enabled: Boolean) {
        val previous = pushNotifications.value
        pushNotifications.value = enabled
        viewModelScope.launch {
            accountRepository.setPushNotifications(enabled)
                .onFailure { pushNotifications.value = previous }
        }
    }

    fun onTermsClick() = openLegalPage(LegalLinks::termsUrl)

    fun onPrivacyClick() = openLegalPage(LegalLinks::privacyUrl)

    fun onLogOutClick() {
        viewModelScope.launch {
            accountRepository.logOut()
            _events.tryEmit(AccountMenuEvent.SessionClosed)
        }
    }

    fun onDeleteAccountClick() {
        deleteConfirmVisible.value = true
    }

    fun onKeepAccountClick() {
        deleteConfirmVisible.value = false
    }

    fun onDeletePermanentlyClick() {
        viewModelScope.launch {
            accountRepository.deleteAccount().onSuccess {
                deleteConfirmVisible.value = false
                _events.tryEmit(AccountMenuEvent.SessionClosed)
            }
        }
    }

    fun onMenuDismissed() {
        deleteConfirmVisible.value = false
    }

    private fun openLegalPage(url: (LegalLinks) -> String) {
        viewModelScope.launch {
            val page = url(accountRepository.legalLinks())
            if (page.isNotBlank()) _events.tryEmit(AccountMenuEvent.OpenUrl(page))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
