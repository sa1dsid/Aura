package com.aura.core.api

import com.aura.feature.home.domain.model.TestStartRejection
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.InviteFailure
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketException
import java.net.UnknownHostException

class ApiErrorsLiveBodiesTest {

    @Test
    fun `register on a taken email`() {
        val body = """{"detail":"Account already exists"}"""

        assertEquals(
            AuthFailure.EMAIL_ALREADY_REGISTERED,
            error(409, body).toAuthFailure().failure,
        )
    }

    @Test
    fun `login with a wrong password`() {
        val body = """{"detail":"Wrong email or password"}"""

        assertEquals(AuthFailure.WRONG_PASSWORD, error(401, body).toAuthFailure().failure)
    }

    @Test
    fun `register with a seven character password`() {
        val body = """
            {"detail":[{"type":"string_too_short","loc":["body","password"],
            "msg":"String should have at least 8 characters","input":"1234567",
            "ctx":{"min_length":8}}]}
        """.trimIndent()

        assertEquals(AuthFailure.PASSWORD_TOO_SHORT, error(422, body).toAuthFailure().failure)
    }

    @Test
    fun `register with an address that has no at sign`() {
        val body = """
            {"detail":[{"type":"value_error","loc":["body","email"],
            "msg":"value is not a valid email address: An email address must have an @-sign.",
            "input":"notanemail","ctx":{"reason":"An email address must have an @-sign."}}]}
        """.trimIndent()

        assertEquals(AuthFailure.EMAIL_INVALID, error(422, body).toAuthFailure().failure)
    }

    @Test
    fun `a rejected google token does not blame the password field`() {
        val body = """{"detail":"Invalid Google ID token"}"""

        assertEquals(
            AuthFailure.GOOGLE_UNAVAILABLE,
            error(401, body).toAuthFailure(googleSignIn = true).failure,
        )
    }

    @Test
    fun `a missing id token is a validation failure`() {
        val body = """
            {"detail":[{"type":"missing","loc":["body","id_token"],"msg":"Field required",
            "input":{}}]}
        """.trimIndent()

        assertEquals(
            AuthFailure.EMAIL_INVALID,
            error(422, body).toAuthFailure(googleSignIn = true).failure,
        )
    }

    @Test
    fun `a dropped socket during the first google call reads as a network failure`() {
        assertEquals(
            AuthFailure.NETWORK,
            SocketException("Connection reset").toAuthFailure(googleSignIn = true).failure,
        )
        assertEquals(
            AuthFailure.NETWORK,
            UnknownHostException("3.127.248.37").toAuthFailure().failure,
        )
    }

    @Test
    fun `an unparseable error body still reads as a server outage`() {
        assertEquals(
            AuthFailure.SERVER_UNAVAILABLE,
            error(500, "Internal Server Error").toAuthFailure().failure,
        )
    }

    @Test
    fun `an invite code nobody owns`() {
        val body = """{"detail":"Invite code not found"}"""

        assertEquals(InviteFailure.UNKNOWN_CODE, error(404, body).toInviteFailure().failure)
    }

    @Test
    fun `the users own invite code`() {
        val body = """{"detail":"Own invite code is not allowed"}"""

        assertEquals(InviteFailure.OWN_CODE, error(422, body).toInviteFailure().failure)
    }

    @Test
    fun `an invite code of the wrong length`() {
        val body = """
            {"detail":[{"type":"string_too_short","loc":["body","code"],
            "msg":"String should have at least 8 characters","input":"ABC1234",
            "ctx":{"min_length":8}}]}
        """.trimIndent()

        assertEquals(InviteFailure.UNKNOWN_CODE, error(422, body).toInviteFailure().failure)
    }

    @Test
    fun `an invite code with characters outside the pattern`() {
        val body = """
            {"detail":[{"type":"string_pattern_mismatch","loc":["body","code"],
            "msg":"String should match pattern '^[A-Za-z0-9]{8}$'","input":"' OR 1=1",
            "ctx":{"pattern":"^[A-Za-z0-9]{8}$"}}]}
        """.trimIndent()

        assertEquals(InviteFailure.UNKNOWN_CODE, error(422, body).toInviteFailure().failure)
    }

    @Test
    fun `a second invite decision`() {
        assertEquals(
            InviteFailure.ALREADY_APPLIED,
            error(409, """{"detail":"Invite decision is permanent"}""").toInviteFailure().failure,
        )
        assertEquals(
            InviteFailure.ALREADY_APPLIED,
            error(409, """{"detail":"Manually applied code is permanent"}""")
                .toInviteFailure().failure,
        )
    }

    @Test
    fun `a second tap start while one runs`() {
        val body = """{"detail":"A test is already running"}"""

        assertEquals(TestStartRejection.SessionStuck, error(409, body).toTapRejection())
    }

    @Test
    fun `a finish before the three minutes are up is not a stuck session`() {
        val body = """{"detail":"Test is still running"}"""

        assertEquals(TestStartRejection.Unavailable, error(409, body).toTapRejection())
    }

    @Test
    fun `a tap start behind a vpn`() {
        val body = """{"detail":"ION paused - VPN detected"}"""

        assertEquals(TestStartRejection.VpnDetected, error(409, body).toTapRejection())
    }

    @Test
    fun `a tap start on a device the server refuses`() {
        val body = """{"detail":"ION paused - unsupported device"}"""

        assertEquals(TestStartRejection.UnsupportedDevice, error(409, body).toTapRejection())
    }

    @Test
    fun `a tap start inside the cooldown carries the remaining time`() {
        val body =
            """{"detail":"Next test not ready - come back at 2099-08-25T05:06:43.459334+00:00"}"""

        val rejection = error(409, body).toTapRejection()

        assertTrue(rejection is TestStartRejection.CooldownNotFinished)
        assertTrue((rejection as TestStartRejection.CooldownNotFinished).remaining.isPositive())
    }

    @Test
    fun `a session id the server does not know`() {
        val body = """{"detail":"Test session not found"}"""

        assertEquals(TestStartRejection.Unavailable, error(404, body).toTapRejection())
    }

    @Test
    fun `an offline start is told apart from a server refusal`() {
        assertEquals(TestStartRejection.NoConnection, SocketException("reset").toTapRejection())
    }

    @Test
    fun `a validation body on tap start does not read as a domain refusal`() {
        val body = """
            {"detail":[{"type":"literal_error","loc":["body","network_type"],
            "msg":"Input should be 'wifi' or 'mobile'","input":"ethernet"}]}
        """.trimIndent()

        assertEquals(TestStartRejection.Unavailable, error(422, body).toTapRejection())
    }

    private fun error(code: Int, body: String) = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType()))
    )
}
