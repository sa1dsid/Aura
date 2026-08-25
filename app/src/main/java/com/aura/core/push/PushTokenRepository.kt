package com.aura.core.push

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.PushTokenDeleteDto
import com.aura.core.api.dto.PushTokenRegisterDto
import com.aura.core.common.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PLATFORM_ANDROID = "android"

@Singleton
class PushTokenRepository @Inject constructor(
    private val api: AuraApi,
    private val provider: PushTokenProvider,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun register(): Boolean = withContext(ioDispatcher) {
        val token = provider.token() ?: return@withContext false

        try {
            api.registerPushToken(
                PushTokenRegisterDto(token = token, platform = PLATFORM_ANDROID)
            ).registered
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            false
        }
    }

    suspend fun remove(): Boolean = withContext(ioDispatcher) {
        val token = provider.token() ?: return@withContext false

        try {
            api.removePushToken(PushTokenDeleteDto(token = token)).removed
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            false
        }
    }

    suspend fun sync(enabled: Boolean) {
        if (enabled) register() else remove()
    }

    suspend fun refresh() {
        val enabled = withContext(ioDispatcher) {
            try {
                api.currentUser().pushEnabled
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                null
            }
        } ?: return

        sync(enabled)
    }
}
