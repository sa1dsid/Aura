package com.aura.core.api

import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

private const val BAD_REQUEST = 400

private const val UNAUTHORIZED = 401

private const val NOT_FOUND = 404

private const val CONFLICT = 409

private const val UNPROCESSABLE = 422

private const val SERVICE_UNAVAILABLE = 503

private val errorJson = Json { ignoreUnknownKeys = true }

private class ApiError(val code: Int, val detail: String?, val fields: List<String>)

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

    return ApiError(code = http.code(), detail = text, fields = fields)
}

fun Throwable.toAuthFailure(): AuthException {
    if (this is AuthException) return this
    val error = apiError() ?: return AuthException(AuthFailure.NETWORK)

    return AuthException(
        when (error.code) {
            UNAUTHORIZED -> AuthFailure.WRONG_PASSWORD
            CONFLICT -> AuthFailure.EMAIL_ALREADY_REGISTERED
            UNPROCESSABLE -> when {
                "password" in error.fields -> AuthFailure.PASSWORD_TOO_SHORT
                else -> AuthFailure.EMAIL_INVALID
            }

            SERVICE_UNAVAILABLE -> AuthFailure.GOOGLE_UNAVAILABLE
            else -> AuthFailure.NETWORK
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
                else -> InviteFailure.OWN_CODE
            }

            BAD_REQUEST -> InviteFailure.UNKNOWN_CODE
            else -> InviteFailure.NETWORK
        }
    )
}
