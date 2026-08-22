package com.aura.feature.account.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.PreferenceUpdateDto
import com.aura.feature.account.data.remote.dto.AccountSettingsDto
import com.aura.feature.account.data.remote.dto.LegalLinksDto
import javax.inject.Inject
import javax.inject.Singleton

interface AccountRemoteDataSource {
    suspend fun settings(accountId: String): AccountSettingsDto

    suspend fun updatePushNotifications(accountId: String, enabled: Boolean)

    suspend fun legalLinks(): LegalLinksDto

    suspend fun deleteAccount(accountId: String)
}

@Singleton
class ApiAccountRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : AccountRemoteDataSource {

    override suspend fun settings(accountId: String): AccountSettingsDto =
        AccountSettingsDto(pushNotifications = api.currentUser().pushEnabled)

    override suspend fun updatePushNotifications(accountId: String, enabled: Boolean) {
        api.updatePreferences(PreferenceUpdateDto(pushEnabled = enabled))
    }

    override suspend fun legalLinks(): LegalLinksDto {
        val config = api.publicConfig()
        return LegalLinksDto(termsUrl = config.termsUrl, privacyUrl = config.privacyUrl)
    }

    override suspend fun deleteAccount(accountId: String) {
        api.deleteCurrentUser()
    }
}
