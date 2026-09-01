package com.aura.feature.onboarding.domain.usecase

import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.model.EmailVerificationException
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import com.aura.feature.onboarding.domain.model.MIN_PASSWORD_LENGTH
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.model.isWholeEmailCode
import com.aura.feature.onboarding.domain.model.isWholeInviteCode
import com.aura.feature.onboarding.domain.model.toEmailCode
import com.aura.feature.onboarding.domain.model.toInviteCode
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.BootRepository
import com.aura.feature.onboarding.domain.repository.InviteRepository
import javax.inject.Inject

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$")

private fun String.asEmailAddress(): String? = trim().takeIf(EMAIL_PATTERN::matches)

private fun rejected(failure: AuthFailure): Result<Nothing> =
    Result.failure(AuthException(failure))

class BootstrapUseCase @Inject constructor(
    private val bootRepository: BootRepository,
) {
    suspend operator fun invoke(): BootConfig = bootRepository.bootstrap()
}

class ResolveStartDestinationUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): StartDestination = authRepository.restoreSession()
}

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession> {
        val address = email.asEmailAddress() ?: return rejected(AuthFailure.EMAIL_INVALID)
        return authRepository.signIn(address, password)
    }
}

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<EmailVerification> {
        val address = email.asEmailAddress() ?: return rejected(AuthFailure.EMAIL_INVALID)
        if (password.length < MIN_PASSWORD_LENGTH) {
            return rejected(AuthFailure.PASSWORD_TOO_SHORT)
        }
        return authRepository.signUp(address, password)
    }
}

class ConfirmEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, code: String): Result<AuthSession> {
        val digits = code.toEmailCode()
        if (!digits.isWholeEmailCode) {
            return Result.failure(EmailVerificationException(EmailVerificationFailure.CODE_REJECTED))
        }
        return authRepository.confirmEmail(email, digits)
    }
}

class ResendEmailCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> =
        authRepository.resendEmailCode(email)
}

class ContinueWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String): Result<AuthSession> =
        authRepository.continueWithGoogle(idToken)
}

class RequestPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return rejected(AuthFailure.EMAIL_REQUIRED)
        val address = email.asEmailAddress() ?: return rejected(AuthFailure.EMAIL_INVALID)
        return authRepository.requestPasswordReset(address)
    }
}

class ObserveInviteAttributionUseCase @Inject constructor(
    private val inviteRepository: InviteRepository,
) {
    suspend operator fun invoke(): InviteAttribution = inviteRepository.pendingAttribution()
}

class ApplyInviteCodeUseCase @Inject constructor(
    private val inviteRepository: InviteRepository,
) {
    suspend operator fun invoke(code: String): Result<Unit> {
        val inviteCode = code.toInviteCode()
        if (!inviteCode.isWholeInviteCode) {
            return Result.failure(InviteException(InviteFailure.UNKNOWN_CODE))
        }
        return inviteRepository.applyCode(inviteCode)
    }
}

class SkipInviteUseCase @Inject constructor(
    private val inviteRepository: InviteRepository,
) {
    suspend operator fun invoke(): Result<Unit> = inviteRepository.skipInvite()
}
