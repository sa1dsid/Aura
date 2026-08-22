package com.aura.core.api

import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.InviteFailure
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorsTest {

    @Test
    fun `login with a wrong password reads as wrong credentials`() {
        val failure = httpError(401, """{"detail":"Wrong email or password"}""").toAuthFailure()

        assertEquals(AuthFailure.WRONG_PASSWORD, failure.failure)
    }

    @Test
    fun `register on a taken email reads as an existing account`() {
        val failure = httpError(409, """{"detail":"Account already exists"}""").toAuthFailure()

        assertEquals(AuthFailure.EMAIL_ALREADY_REGISTERED, failure.failure)
    }

    @Test
    fun `a short password is reported on the password field`() {
        val body = """
            {"detail":[{"type":"string_too_short","loc":["body","password"],
            "msg":"String should have at least 8 characters"}]}
        """.trimIndent()

        assertEquals(AuthFailure.PASSWORD_TOO_SHORT, httpError(422, body).toAuthFailure().failure)
    }

    @Test
    fun `a malformed email is reported on the email field`() {
        val body = """
            {"detail":[{"type":"value_error","loc":["body","email"],
            "msg":"value is not a valid email address"}]}
        """.trimIndent()

        assertEquals(AuthFailure.EMAIL_INVALID, httpError(422, body).toAuthFailure().failure)
    }

    @Test
    fun `a failed google verification is not reported as a dead network`() {
        val body = """{"detail":"Google verification unavailable"}"""

        assertEquals(AuthFailure.GOOGLE_UNAVAILABLE, httpError(503, body).toAuthFailure().failure)
    }

    @Test
    fun `a rejected google token is not reported as a wrong password`() {
        val body = """{"detail":"Invalid Google token"}"""

        assertEquals(
            AuthFailure.GOOGLE_UNAVAILABLE,
            httpError(401, body).toAuthFailure(googleSignIn = true).failure,
        )
    }

    @Test
    fun `a dropped connection reads as a network failure`() {
        assertEquals(AuthFailure.NETWORK, IOException("offline").toAuthFailure().failure)
    }

    @Test
    fun `an unknown invite code reads as unknown`() {
        val body = """{"detail":"Invite code not found"}"""

        assertEquals(InviteFailure.UNKNOWN_CODE, httpError(404, body).toInviteFailure().failure)
    }

    @Test
    fun `the own invite code is told apart from a validation error`() {
        val body = """{"detail":"Own invite code is not allowed"}"""

        assertEquals(InviteFailure.OWN_CODE, httpError(422, body).toInviteFailure().failure)
    }

    @Test
    fun `a too short invite code is a validation error, not the own code`() {
        val body = """
            {"detail":[{"type":"string_too_short","loc":["body","code"],
            "msg":"String should have at least 8 characters"}]}
        """.trimIndent()

        assertEquals(InviteFailure.UNKNOWN_CODE, httpError(422, body).toInviteFailure().failure)
    }

    @Test
    fun `a second invite decision reads as already applied`() {
        val body = """{"detail":"Invite decision is permanent"}"""

        assertEquals(InviteFailure.ALREADY_APPLIED, httpError(409, body).toInviteFailure().failure)
    }

    private fun httpError(code: Int, body: String) = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType()))
    )
}
