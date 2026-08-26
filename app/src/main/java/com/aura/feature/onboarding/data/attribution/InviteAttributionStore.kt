package com.aura.feature.onboarding.data.attribution

import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.isWholeInviteCode
import com.aura.feature.onboarding.domain.model.toInviteCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

interface InstallReferrerSource {
    suspend fun inviteCode(): String?
}

data class InviteAttributionState(
    val deepLinkCode: String? = null,
    val referrerCode: String? = null,
    val referrerRead: Boolean = false,
    val consumed: Boolean = false,
)

interface InviteAttributionStorage {
    suspend fun read(): InviteAttributionState

    suspend fun write(state: InviteAttributionState)
}

@Singleton
class InviteAttributionStore @Inject constructor(
    private val installReferrer: InstallReferrerSource,
    private val storage: InviteAttributionStorage,
) {

    private val mutex = Mutex()

    private var cached: InviteAttributionState? = null

    suspend fun pending(): InviteAttribution = mutex.withLock {
        val known = state()
        if (known.consumed) return@withLock InviteAttribution.None

        val state = if (known.referrerRead) {
            known
        } else {
            save(
                known.copy(
                    referrerRead = true,
                    referrerCode = installReferrer.inviteCode()?.asInviteCode(),
                )
            )
        }

        val code = state.deepLinkCode ?: state.referrerCode
        if (code == null) InviteAttribution.None else InviteAttribution.FromLink(code)
    }

    suspend fun rememberDeepLink(rawCode: String) = mutex.withLock {
        val state = state()
        if (state.consumed) return@withLock
        val code = rawCode.asInviteCode() ?: return@withLock
        save(state.copy(deepLinkCode = code))
        Unit
    }

    suspend fun consume() = mutex.withLock {
        save(InviteAttributionState(referrerRead = true, consumed = true))
        Unit
    }

    private suspend fun state(): InviteAttributionState =
        cached ?: storage.read().also { cached = it }

    private suspend fun save(state: InviteAttributionState): InviteAttributionState {
        cached = state
        storage.write(state)
        return state
    }
}

private fun String.asInviteCode(): String? = toInviteCode().takeIf { it.isWholeInviteCode }
