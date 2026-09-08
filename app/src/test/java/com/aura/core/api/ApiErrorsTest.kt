package com.aura.core.api

import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.domain.model.InviteException
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
    fun `login on an unconfirmed account reads as an unverified email`() {
        val body = """{"detail":"Email verification required"}"""

        assertEquals(AuthFailure.EMAIL_NOT_VERIFIED, httpError(403, body).toAuthFailure().failure)
    }

    @Test
    fun `a google sign in the server forbids never asks for an email code`() {
        val body = """{"detail":"Email verification required"}"""

        assertEquals(
            AuthFailure.GOOGLE_UNAVAILABLE,
            httpError(403, body).toAuthFailure(googleSignIn = true).failure,
        )
    }

    @Test
    fun `an expired confirmation code reads as a rejected code`() {
        val body = """{"detail":"Invalid or expired confirmation code"}"""

        assertEquals(
            EmailVerificationFailure.CODE_REJECTED,
            httpError(400, body).toEmailVerificationFailure().failure,
        )
    }

    @Test
    fun `a code that is not six digits reads as a rejected code`() {
        val body = """
            {"detail":[{"type":"string_pattern_mismatch","loc":["body","code"],
            "msg":"String should match pattern"}]}
        """.trimIndent()

        assertEquals(
            EmailVerificationFailure.CODE_REJECTED,
            httpError(422, body).toEmailVerificationFailure().failure,
        )
    }

    @Test
    fun `a throttled resend is told apart from a dead network`() {
        val body = """{"detail":"Please wait before requesting another code"}"""

        assertEquals(
            EmailVerificationFailure.RESEND_TOO_SOON,
            httpError(429, body).toEmailVerificationFailure().failure,
        )
        assertEquals(
            EmailVerificationFailure.NETWORK,
            IOException("offline").toEmailVerificationFailure().failure,
        )
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

        assertEquals(
            AuthFailure.GOOGLE_UNAVAILABLE,
            httpError(503, body).toAuthFailure(googleSignIn = true).failure,
        )
    }

    @Test
    fun `a backend outage on the email path does not blame the network`() {
        val body = """{"detail":"Service Unavailable"}"""

        assertEquals(AuthFailure.SERVER_UNAVAILABLE, httpError(503, body).toAuthFailure().failure)
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


    @Test
    fun `a malformed invite code rejected up front reads as unknown`() {
        val body = """{"detail":"Invite code must be 8 characters"}"""

        assertEquals(InviteFailure.UNKNOWN_CODE, httpError(400, body).toInviteFailure().failure)
    }

    @Test
    fun `a status nobody mapped reads as a network failure`() {
        assertEquals(AuthFailure.NETWORK, httpError(418, "{}").toAuthFailure().failure)
        assertEquals(AuthFailure.NETWORK, httpError(404, "{}").toAuthFailure().failure)
        assertEquals(InviteFailure.NETWORK, httpError(418, "{}").toInviteFailure().failure)
        assertEquals(InviteFailure.NETWORK, httpError(500, "{}").toInviteFailure().failure)
    }

    @Test
    fun `a server status reads as an outage and not as a dead network`() {
        assertEquals(AuthFailure.SERVER_UNAVAILABLE, httpError(500, "{}").toAuthFailure().failure)
        assertEquals(AuthFailure.SERVER_UNAVAILABLE, httpError(504, "{}").toAuthFailure().failure)
    }

    @Test
    fun `an unreadable body on a validation status blames nobody`() {
        val proxyPage = "<html>bad gateway</html>"

        assertEquals(AuthFailure.NETWORK, httpError(422, proxyPage).toAuthFailure().failure)
        assertEquals(InviteFailure.NETWORK, httpError(422, proxyPage).toInviteFailure().failure)
    }

    @Test
    fun `an unreadable body on any other status keeps the status meaning`() {
        val proxyPage = "<html>bad gateway</html>"

        assertEquals(AuthFailure.SERVER_UNAVAILABLE, httpError(502, proxyPage).toAuthFailure().failure)
        assertEquals(InviteFailure.NETWORK, httpError(500, proxyPage).toInviteFailure().failure)
    }

    @Test
    fun `a failure already named by the app is passed through untouched`() {
        val auth = AuthException(AuthFailure.GOOGLE_CANCELLED)
        val invite = InviteException(InviteFailure.OWN_CODE)

        assertEquals(AuthFailure.GOOGLE_CANCELLED, auth.toAuthFailure().failure)
        assertEquals(InviteFailure.OWN_CODE, invite.toInviteFailure().failure)
    }

    @Test
    fun `a validation error without a type still reads as a short password`() {
        val body = """{"detail":[{"loc":["body","password"],"msg":"too short"}]}"""

        assertEquals(AuthFailure.PASSWORD_TOO_SHORT, httpError(422, body).toAuthFailure().failure)
    }

    @Test
    fun `a validation error naming no field at all is not pinned on the email`() {
        val body = """{"detail":[{"type":"value_error","loc":[],"msg":"bad"}]}"""

        assertEquals(AuthFailure.NETWORK, httpError(422, body).toAuthFailure().failure)
    }

    private fun httpError(code: Int, body: String) = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType()))
    )
}
