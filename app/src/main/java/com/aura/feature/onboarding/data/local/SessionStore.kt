package com.aura.feature.onboarding.data.local

import com.aura.feature.onboarding.domain.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStore @Inject constructor() {

    private val current = MutableStateFlow<Account?>(null)

    val account: StateFlow<Account?> = current.asStateFlow()

    fun open(account: Account) {
        current.value = account
    }

    fun close() {
        current.value = null
    }
}
