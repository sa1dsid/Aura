package com.aura.feature.account.data.remote

import com.aura.feature.account.data.remote.dto.AccountSettingsDto
import com.aura.feature.account.data.remote.dto.LegalLinksDto
import com.aura.feature.onboarding.data.remote.OnboardingBackend
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface AccountRemoteDataSource {
    suspend fun settings(accountId: String): AccountSettingsDto

    suspend fun updatePushNotifications(accountId: String, enabled: Boolean)

    suspend fun legalLinks(): LegalLinksDto

    suspend fun deleteAccount(accountId: String)
}

@Singleton
class MockAccountRemoteDataSource @Inject constructor(
    private val backend: OnboardingBackend,
) : AccountRemoteDataSource {

    override suspend fun settings(accountId: String): AccountSettingsDto {
        delay(SETTINGS_DELAY_MILLIS)
        return AccountSettingsDto(pushNotifications = backend.pushNotifications(accountId))
    }

    override suspend fun updatePushNotifications(accountId: String, enabled: Boolean) {
        delay(NETWORK_DELAY_MILLIS)
        backend.setPushNotifications(accountId, enabled)
    }

    override suspend fun legalLinks(): LegalLinksDto {
        delay(SETTINGS_DELAY_MILLIS)
        return LegalLinksDto(termsUrl = TERMS_URL, privacyUrl = PRIVACY_URL)
    }

    override suspend fun deleteAccount(accountId: String) {
        delay(NETWORK_DELAY_MILLIS)
        backend.deleteAccount(accountId)
    }

    private companion object {
        const val NETWORK_DELAY_MILLIS = 600L
        const val SETTINGS_DELAY_MILLIS = 200L
        const val TERMS_URL = "https://ioaura.app/terms"
        const val PRIVACY_URL = "https://ioaura.app/privacy"
    }
}
