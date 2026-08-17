package com.aura.feature.onboarding.data.remote

import com.aura.feature.onboarding.data.remote.dto.AccountDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.INVITE_CODE_LENGTH
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import com.aura.feature.onboarding.domain.model.MIN_PASSWORD_LENGTH
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val WELCOME_BONUS_ION = 3_000

private const val INVITE_LINK_PREFIX = "https://ioaura.app/i/"

private const val SEED_EMAIL = "syrex@ioaura.app"

const val SEED_INVITE_CODE = "SYREX482"

@Singleton
class OnboardingBackend @Inject constructor() {

    private class Record(
        val id: String,
        val email: String,
        val handle: String,
        val inviteCode: String,
        val passwordHash: Int?,
        val authProvider: AuthProvider,
        var deleted: Boolean = false,
        var invitedByCode: String? = null,
        var inviteSkipped: Boolean = false,
        var inviteScreenPassed: Boolean = false,
        var bonusPopupShown: Boolean = false,
        var reservedBonusIon: Int = 0,
        var pushNotifications: Boolean = true,
    )

    private val mutex = Mutex()
    private val records = linkedMapOf<String, Record>()

    init {
        records[SEED_EMAIL] = Record(
            id = "acc_0_syrex",
            email = SEED_EMAIL,
            handle = "syrex",
            inviteCode = SEED_INVITE_CODE,
            passwordHash = null,
            authProvider = AuthProvider.GOOGLE,
            inviteSkipped = true,
            inviteScreenPassed = true,
            bonusPopupShown = true,
        )
    }

    suspend fun signIn(email: String, password: String): AccountDto = mutex.withLock {
        val record = records[email.lowercase()]?.takeIf { !it.deleted }
            ?: throw AuthException(AuthFailure.ACCOUNT_NOT_FOUND)
        if (record.passwordHash != null && record.passwordHash != password.hashCode()) {
            throw AuthException(AuthFailure.WRONG_PASSWORD)
        }
        record.toDto()
    }

    suspend fun signUp(email: String, password: String): AccountDto = mutex.withLock {
        if (password.length < MIN_PASSWORD_LENGTH) {
            throw AuthException(AuthFailure.PASSWORD_TOO_SHORT)
        }
        val key = email.lowercase()
        if (records[key]?.deleted == false) {
            throw AuthException(AuthFailure.EMAIL_ALREADY_REGISTERED)
        }
        createRecord(key, password.hashCode(), AuthProvider.EMAIL).toDto()
    }

    suspend fun signInWithGoogle(email: String): Pair<AccountDto, Boolean> = mutex.withLock {
        val key = email.lowercase()
        val existing = records[key]?.takeIf { !it.deleted }
        if (existing != null) return@withLock existing.toDto() to false
        createRecord(key, passwordHash = null, authProvider = AuthProvider.GOOGLE).toDto() to true
    }

    suspend fun pushNotifications(accountId: String): Boolean = mutex.withLock {
        requireRecord(accountId).pushNotifications
    }

    suspend fun setPushNotifications(accountId: String, enabled: Boolean) = mutex.withLock {
        requireRecord(accountId).pushNotifications = enabled
    }

    suspend fun deleteAccount(accountId: String) = mutex.withLock {
        val record = requireRecord(accountId)
        record.deleted = true
        record.invitedByCode = null
        record.reservedBonusIon = 0
    }

    suspend fun isRegistered(email: String): Boolean = mutex.withLock {
        records[email.lowercase()]?.deleted == false
    }

    suspend fun flags(accountId: String): OnboardingFlagsDto = mutex.withLock {
        val record = requireRecord(accountId)
        OnboardingFlagsDto(
            inviteScreenPassed = record.inviteScreenPassed,
            bonusPopupShown = record.bonusPopupShown,
            reservedBonusIon = record.reservedBonusIon,
        )
    }

    suspend fun applyInviteCode(accountId: String, code: String) = mutex.withLock {
        val record = requireRecord(accountId)
        if (record.inviteScreenPassed || record.inviteSkipped) {
            throw InviteException(InviteFailure.ALREADY_APPLIED)
        }
        if (code == record.inviteCode) throw InviteException(InviteFailure.OWN_CODE)
        val owner = records.values.firstOrNull { it.inviteCode == code }
            ?: throw InviteException(InviteFailure.UNKNOWN_CODE)
        if (owner.deleted) throw InviteException(InviteFailure.OWNER_DELETED)
        record.invitedByCode = code
        record.inviteScreenPassed = true
    }

    suspend fun skipInvite(accountId: String) = mutex.withLock {
        val record = requireRecord(accountId)
        record.invitedByCode = null
        record.inviteSkipped = true
        record.inviteScreenPassed = true
    }

    suspend fun markBonusPopupShown(accountId: String) = mutex.withLock {
        requireRecord(accountId).bonusPopupShown = true
    }

    suspend fun invitedByCode(accountId: String): String? = mutex.withLock {
        requireRecord(accountId).invitedByCode
    }

    private fun requireRecord(accountId: String): Record =
        records.values.firstOrNull { it.id == accountId && !it.deleted }
            ?: throw AuthException(AuthFailure.ACCOUNT_NOT_FOUND)

    private fun createRecord(
        email: String,
        passwordHash: Int?,
        authProvider: AuthProvider,
    ): Record {
        val handle = uniqueHandle(email.substringBefore('@'))
        val record = Record(
            id = "acc_${records.size + 1}_$handle",
            email = email,
            handle = handle,
            inviteCode = inviteCodeFor(handle),
            passwordHash = passwordHash,
            authProvider = authProvider,
            reservedBonusIon = WELCOME_BONUS_ION,
        )
        records[email] = record
        return record
    }

    private fun uniqueHandle(raw: String): String {
        val base = raw.lowercase().filter { it.isLetterOrDigit() }.ifEmpty { "node" }
        if (records.values.none { it.handle == base }) return base
        var index = 2
        while (records.values.any { it.handle == "$base$index" }) index++
        return "$base$index"
    }

    private fun inviteCodeFor(handle: String): String {
        val letters = handle.uppercase().filter { it.isLetter() }.take(5).padEnd(5, 'X')
        val digits = (handle.hashCode().toLong() and 0xFFF).toString().padStart(3, '0').takeLast(3)
        return (letters + digits).take(INVITE_CODE_LENGTH)
    }

    private fun Record.toDto() = AccountDto(
        id = id,
        email = email,
        handle = handle,
        inviteCode = inviteCode,
        inviteLink = INVITE_LINK_PREFIX + inviteCode,
        authProvider = authProvider.name,
    )
}
