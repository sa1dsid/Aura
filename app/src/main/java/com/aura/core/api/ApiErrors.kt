package com.aura.core.api

import com.aura.core.common.parseIsoMillis
import com.aura.feature.home.domain.model.TestStartRejection
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.EmailVerificationException
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import kotlin.time.Duration.Companion.milliseconds

private const val BAD_REQUEST = 400

private const val UNAUTHORIZED = 401

private const val FORBIDDEN = 403

private const val NOT_FOUND = 404

private const val CONFLICT = 409

private const val UNPROCESSABLE = 422

private const val TOO_MANY_REQUESTS = 429

private const val TOO_LONG = "too_long"

private const val SERVICE_UNAVAILABLE = 503

private val errorJson = Json { ignoreUnknownKeys = true }

private class ApiError(
    val code: Int,
    val detail: String?,
    val fields: List<String>,
    val types: List<String>,
) {
    val namesTheProblem: Boolean
        get() = fields.isNotEmpty() || detail != null
}

private fun Throwable.apiError(): ApiError? {
    val http = this as? HttpException ?: return null
    val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
    val detail = body
        ?.let { runCatching { errorJson.parseToJsonElement(it).jsonObject["detail"] }.getOrNull() }

    val text = detail?.let { element ->
        runCatching { element.jsonPrimitive.content }.getOrNull()
    }
    val fields = (detail as? JsonArray)
        ?.mapNotNull { entry ->
            runCatching {
                entry.jsonObject["loc"]?.jsonArray?.last()?.jsonPrimitive?.content
            }.getOrNull()
        }
        .orEmpty()

    val types = (detail as? JsonArray)
        ?.mapNotNull { entry ->
            runCatching { entry.jsonObject["type"]?.jsonPrimitive?.content }.getOrNull()
        }
        .orEmpty()

    return ApiError(code = http.code(), detail = text, fields = fields, types = types)
}

fun Throwable.toAuthFailure(googleSignIn: Boolean = false): AuthException {
    if (this is AuthException) return this
    val error = apiError() ?: return AuthException(AuthFailure.NETWORK)

    return AuthException(
        when (error.code) {
            UNAUTHORIZED -> if (googleSignIn) {
                AuthFailure.GOOGLE_UNAVAILABLE
            } else {
                AuthFailure.WRONG_PASSWORD
            }

            FORBIDDEN -> if (googleSignIn) {
                AuthFailure.GOOGLE_UNAVAILABLE
            } else {
                AuthFailure.EMAIL_NOT_VERIFIED
            }

            CONFLICT -> AuthFailure.EMAIL_ALREADY_REGISTERED
            UNPROCESSABLE -> when {
                "password" in error.fields -> if (error.types.any { it.contains(TOO_LONG) }) {
                    AuthFailure.PASSWORD_TOO_LONG
                } else {
                    AuthFailure.PASSWORD_TOO_SHORT
                }

                error.namesTheProblem -> AuthFailure.EMAIL_INVALID
                else -> AuthFailure.NETWORK
            }

            SERVICE_UNAVAILABLE -> AuthFailure.GOOGLE_UNAVAILABLE
            else -> AuthFailure.NETWORK
        }
    )
}

fun Throwable.toEmailVerificationFailure(): EmailVerificationException {
    if (this is EmailVerificationException) return this
    val error = apiError() ?: return EmailVerificationException(EmailVerificationFailure.NETWORK)

    return EmailVerificationException(
        when (error.code) {
            BAD_REQUEST,
            NOT_FOUND,
            UNPROCESSABLE,
            -> EmailVerificationFailure.CODE_REJECTED

            TOO_MANY_REQUESTS -> EmailVerificationFailure.RESEND_TOO_SOON
            else -> EmailVerificationFailure.NETWORK
        }
    )
}

fun Throwable.toInviteFailure(): InviteException {
    if (this is InviteException) return this
    val error = apiError() ?: return InviteException(InviteFailure.NETWORK)

    return InviteException(
        when (error.code) {
            NOT_FOUND -> InviteFailure.UNKNOWN_CODE
            CONFLICT -> InviteFailure.ALREADY_APPLIED
            UNPROCESSABLE -> when {
                error.fields.isNotEmpty() -> InviteFailure.UNKNOWN_CODE
                error.detail != null -> InviteFailure.OWN_CODE
                else -> InviteFailure.NETWORK
            }

            BAD_REQUEST -> InviteFailure.UNKNOWN_CODE
            else -> InviteFailure.NETWORK
        }
    )
}

private const val DETAIL_VPN = "VPN detected"

private const val DETAIL_UNSUPPORTED_DEVICE = "unsupported device"

private const val DETAIL_COOLDOWN = "come back at"

private const val DETAIL_ALREADY_RUNNING = "already running"

fun Throwable.toTapRejection(): TestStartRejection {
    val error = apiError() ?: return TestStartRejection.NoConnection
    val detail = error.detail.orEmpty()

    return when {
        detail.contains(DETAIL_ALREADY_RUNNING) -> TestStartRejection.SessionStuck

        detail.contains(DETAIL_VPN) -> TestStartRejection.VpnDetected

        detail.contains(DETAIL_UNSUPPORTED_DEVICE) -> TestStartRejection.UnsupportedDevice

        detail.contains(DETAIL_COOLDOWN) -> {
            val availableAt = detail.substringAfter(DETAIL_COOLDOWN).trim().parseIsoMillis()
            val remaining = availableAt?.minus(System.currentTimeMillis())?.coerceAtLeast(0)
            TestStartRejection.CooldownNotFinished(
                remaining = (remaining ?: 0).milliseconds,
            )
        }

        else -> TestStartRejection.Unavailable
    }
}
