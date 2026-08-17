package com.aura.feature.account.domain.repository

import com.aura.feature.account.domain.model.AccountProfile
import com.aura.feature.account.domain.model.LegalLinks
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    val profile: Flow<AccountProfile?>

    suspend fun pushNotifications(): Result<Boolean>

    suspend fun setPushNotifications(enabled: Boolean): Result<Unit>

    suspend fun legalLinks(): LegalLinks

    suspend fun logOut()

    suspend fun deleteAccount(): Result<Unit>
}
