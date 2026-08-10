package com.aura.feature.onboarding.data.attribution

import com.aura.feature.onboarding.domain.model.INVITE_CODE_LENGTH
import com.aura.feature.onboarding.domain.model.InviteAttribution
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

interface InstallReferrerSource {
    suspend fun inviteCode(): String?
}

@Singleton
class InviteAttributionStore @Inject constructor(
    private val installReferrer: InstallReferrerSource,
) {

    private val mutex = Mutex()
    private var deepLinkCode: String? = null
    private var referrerCode: String? = null
    private var referrerRead = false
    private var consumed = false

    suspend fun pending(): InviteAttribution = mutex.withLock {
        if (consumed) return@withLock InviteAttribution.None
        if (!referrerRead) {
            referrerRead = true
            referrerCode = installReferrer.inviteCode()?.let(::normalize)
        }
        val code = deepLinkCode ?: referrerCode
        if (code == null) InviteAttribution.None else InviteAttribution.FromLink(code)
    }

    suspend fun rememberDeepLink(rawCode: String) = mutex.withLock {
        if (consumed) return@withLock
        normalize(rawCode)?.let { deepLinkCode = it }
    }

    suspend fun consume() = mutex.withLock {
        consumed = true
        deepLinkCode = null
        referrerCode = null
    }

    private fun normalize(raw: String): String? = raw.trim().uppercase()
        .filter { it.isLetterOrDigit() }
        .takeIf { it.length == INVITE_CODE_LENGTH }
}
