package com.aura.feature.onboarding.domain.usecase

import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.MIN_PASSWORD_LENGTH
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.domain.repository.AuthRepository
import com.aura.feature.onboarding.domain.repository.BootRepository
import com.aura.feature.onboarding.domain.repository.InviteRepository
import javax.inject.Inject

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$")

class BootstrapUseCase @Inject constructor(
    private val bootRepository: BootRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): BootConfig {
        val config = bootRepository.bootstrap()
        authRepository.currentAccount()
        return config
    }
}

class ResolveStartDestinationUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): StartDestination =
        if (authRepository.currentAccount() == null) StartDestination.AUTH else StartDestination.HOME
}

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession> {
        val trimmed = email.trim()
        if (!EMAIL_PATTERN.matches(trimmed)) {
            return Result.failure(AuthException(AuthFailure.EMAIL_INVALID))
        }
        return authRepository.signIn(trimmed, password)
    }
}

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession> {
        val trimmed = email.trim()
        if (!EMAIL_PATTERN.matches(trimmed)) {
            return Result.failure(AuthException(AuthFailure.EMAIL_INVALID))
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(AuthException(AuthFailure.PASSWORD_TOO_SHORT))
        }
        return authRepository.signUp(trimmed, password)
    }
}

class ContinueWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<AuthSession> = authRepository.continueWithGoogle()
}

class RequestPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(AuthException(AuthFailure.EMAIL_REQUIRED))
        }
        return authRepository.requestPasswordReset(trimmed)
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
    suspend operator fun invoke(accountId: String, code: String): Result<Unit> =
        inviteRepository.applyCode(accountId, code.trim().uppercase())
}

class SkipInviteUseCase @Inject constructor(
    private val inviteRepository: InviteRepository,
) {
    suspend operator fun invoke(accountId: String): Result<Unit> =
        inviteRepository.skipInvite(accountId)
}
