package com.aura.feature.onboarding.presentation.bonus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.OnboardingFlagsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WelcomeBonusEvent {
    data object Finished : WelcomeBonusEvent

    data object SessionLost : WelcomeBonusEvent
}

@HiltViewModel
class WelcomeBonusViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val flagsRepository: OnboardingFlagsRepository,
) : ViewModel() {

    private val _bonusIon = MutableStateFlow(OnboardingFlags.DEFAULT_RESERVED_BONUS_ION)
    val bonusIon: StateFlow<Long> = _bonusIon.asStateFlow()

    private val eventChannel = Channel<WelcomeBonusEvent>(Channel.BUFFERED)
    val events: Flow<WelcomeBonusEvent> = eventChannel.receiveAsFlow()

    private var loadJob: Job? = null

    private var dismissJob: Job? = null

    private var reportedAccountId: String? = null

    fun onScreenResumed() {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            signedInAccountId() ?: return@launch
            _bonusIon.value = flagsRepository.flags().reservedBonusIon
        }
    }

    fun onDismiss() {
        if (dismissJob?.isActive == true) return

        dismissJob = viewModelScope.launch {
            val accountId = signedInAccountId() ?: return@launch
            eventChannel.send(WelcomeBonusEvent.Finished)

            if (accountId == reportedAccountId) return@launch
            reportedAccountId = accountId
            flagsRepository.markBonusPopupShown()
        }
    }

    private suspend fun signedInAccountId(): String? {
        val accountId = authRepository.currentAccount()?.id
        if (accountId == null) eventChannel.send(WelcomeBonusEvent.SessionLost)
        return accountId
    }
}
