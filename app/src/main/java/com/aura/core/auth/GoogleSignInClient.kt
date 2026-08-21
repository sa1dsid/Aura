package com.aura.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.aura.BuildConfig
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInClient @Inject constructor(
    private val credentialManager: CredentialManager,
) {

    suspend fun idToken(activityContext: Context): String {
        if (BuildConfig.GOOGLE_CLIENT_ID.isBlank()) {
            throw AuthException(AuthFailure.NETWORK)
        }

        val credential = try {
            credentialManager.getCredential(
                context = activityContext,
                request = request(filterByAuthorizedAccounts = true),
            ).credential
        } catch (noCredential: NoCredentialException) {
            retryWithAllAccounts(activityContext)
        } catch (cancellation: GetCredentialCancellationException) {
            throw AuthException(AuthFailure.GOOGLE_CANCELLED)
        } catch (failure: GetCredentialException) {
            throw AuthException(AuthFailure.NETWORK)
        }

        return credential.googleIdToken()
    }

    private suspend fun retryWithAllAccounts(activityContext: Context) = try {
        credentialManager.getCredential(
            context = activityContext,
            request = request(filterByAuthorizedAccounts = false),
        ).credential
    } catch (cancellation: GetCredentialCancellationException) {
        throw AuthException(AuthFailure.GOOGLE_CANCELLED)
    } catch (noCredential: NoCredentialException) {
        throw AuthException(AuthFailure.GOOGLE_CANCELLED)
    } catch (failure: GetCredentialException) {
        throw AuthException(AuthFailure.NETWORK)
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

    private fun androidx.credentials.Credential.googleIdToken(): String {
        val custom = this as? CustomCredential
        if (custom?.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException(AuthFailure.NETWORK)
        }
        return GoogleIdTokenCredential.createFrom(custom.data).idToken
    }
}
