package com.aura.core.auth

import android.content.Context
import android.os.Build
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.aura.BuildConfig
import com.aura.core.network.NetworkMonitor
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

private const val ACCOUNT_PICKER_ATTEMPTS = 2

private const val RETRY_DELAY_MILLIS = 400L

private const val WARM_UP_ATTEMPTS = 3

private const val WARM_UP_DELAY_MILLIS = 1_000L

@Singleton
class GoogleSignInClient @Inject constructor(
    private val credentialManager: CredentialManager,
    private val networkMonitor: NetworkMonitor,
) {

    suspend fun warmUp() {
        if (BuildConfig.GOOGLE_CLIENT_ID.isBlank()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return

        repeat(WARM_UP_ATTEMPTS) {
            try {
                credentialManager.prepareGetCredential(request(filterByAuthorizedAccounts = false))
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                delay(WARM_UP_DELAY_MILLIS)
            }
        }
    }

    suspend fun idToken(activityContext: Context): String {
        if (BuildConfig.GOOGLE_CLIENT_ID.isBlank()) {
            throw AuthException(AuthFailure.GOOGLE_UNAVAILABLE)
        }

        val credential = credential(
            activityContext = activityContext,
            filterByAuthorizedAccounts = true,
            attempts = 1,
        ) ?: credential(
            activityContext = activityContext,
            filterByAuthorizedAccounts = false,
            attempts = ACCOUNT_PICKER_ATTEMPTS,
        ) ?: throw AuthException(unavailableFailure())

        return credential.googleIdToken()
    }

    private suspend fun credential(
        activityContext: Context,
        filterByAuthorizedAccounts: Boolean,
        attempts: Int,
    ): Credential? {
        repeat(attempts) { attempt ->
            try {
                return credentialManager.getCredential(
                    context = activityContext,
                    request = request(filterByAuthorizedAccounts),
                ).credential
            } catch (cancellation: GetCredentialCancellationException) {
                throw AuthException(AuthFailure.GOOGLE_CANCELLED)
            } catch (noCredential: NoCredentialException) {
                if (attempt == attempts - 1) return null
            } catch (failure: GetCredentialException) {
                if (attempt == attempts - 1) return null
            }
            delay(RETRY_DELAY_MILLIS)
        }
        return null
    }

    private fun unavailableFailure(): AuthFailure = if (networkMonitor.current().isOnline) {
        AuthFailure.GOOGLE_UNAVAILABLE
    } else {
        AuthFailure.NETWORK
    }

    private fun request(filterByAuthorizedAccounts: Boolean) = GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
        )
        .build()

    private fun Credential.googleIdToken(): String {
        val custom = this as? CustomCredential
        if (custom?.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException(AuthFailure.GOOGLE_UNAVAILABLE)
        }
        return GoogleIdTokenCredential.createFrom(custom.data).idToken
    }
}
