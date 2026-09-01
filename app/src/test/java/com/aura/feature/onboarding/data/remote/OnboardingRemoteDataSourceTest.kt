package com.aura.feature.onboarding.data.remote

import com.aura.core.api.Bodies
import com.aura.core.api.RoutingApiServer
import com.aura.core.config.AppConfigRepository
import com.aura.feature.onboarding.FakeTokenStore
import com.aura.feature.onboarding.Paths
import com.aura.feature.onboarding.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRemoteDataSourceTest {

    private val server = RoutingApiServer()

    private val tokenStore = FakeTokenStore()

    private val remote = ApiOnboardingRemoteDataSource(
        api = server.api,
        tokenStore = tokenStore.store,
        appConfigRepository = AppConfigRepository(server.api, Dispatchers.Unconfined),
    )

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `signing in stores the token the server issued`() = runTest {
        server.next(
            Paths.LOGIN,
            body = Server.token(accessToken = "issued.token", expiresIn = 3_600),
        )

        remote.signIn("said@ioaura.app", "Password123")

        assertEquals("issued.token", tokenStore.token)
        assertEquals(3_600, tokenStore.expiresIn)
    }

    @Test
    fun `signing up stores no token and reports the code lifetime`() = runTest {
        server.next(
            Paths.REGISTER,
            code = 201,
            body = Server.verificationPending(email = "said@ioaura.app", expiresIn = 600),
        )

        val pending = remote.signUp("said@ioaura.app", "Password123")

        assertNull(tokenStore.token)
        assertEquals("said@ioaura.app", pending.email)
        assertEquals(600, pending.expiresInSeconds)
    }

    @Test
    fun `confirming the code stores the token the server issues`() = runTest {
        server.next(
            Paths.EMAIL_CONFIRM,
            body = Server.token(accessToken = "fresh.token", expiresIn = 3_600),
        )

        remote.confirmEmail("said@ioaura.app", "482913")

        assertEquals("fresh.token", tokenStore.token)
        assertEquals(3_600, tokenStore.expiresIn)
        assertEquals(
            """{"email":"said@ioaura.app","code":"482913"}""",
            server.bodyOf(Paths.EMAIL_CONFIRM),
        )
    }

    @Test
    fun `resending the code posts only the address`() = runTest {
        server.next(Paths.EMAIL_RESEND, body = Server.message())

        remote.resendEmailCode("said@ioaura.app")

        assertNull(tokenStore.token)
        assertEquals("""{"email":"said@ioaura.app"}""", server.bodyOf(Paths.EMAIL_RESEND))
    }

    @Test
    fun `google sign in posts the id token`() = runTest {
        server.next(Paths.GOOGLE, body = Server.token())

        remote.signInWithGoogle("google.id.token")

        assertEquals("""{"id_token":"google.id.token"}""", server.bodyOf(Paths.GOOGLE))
    }

    @Test
    fun `an account is built out of the user the server describes`() = runTest {
        server.next(
            Paths.LOGIN,
            body = Server.token(
                user = Server.user(
                    id = 77,
                    email = "said@ioaura.app",
                    displayName = "said",
                    authMethods = listOf("email", "google"),
                    promoCode = "SYREX482",
                )
            ),
        )

        val account = remote.signIn("said@ioaura.app", "Password123").account

        assertEquals("77", account.id)
        assertEquals("said@ioaura.app", account.email)
        assertEquals("said", account.handle)
        assertEquals("https://ioaura.app/i/SYREX482", account.inviteLink)
        assertEquals("GOOGLE", account.authProvider)
    }

    @Test
    fun `an account without a google method is an email account`() = runTest {
        server.next(
            Paths.LOGIN,
            body = Server.token(user = Server.user(authMethods = listOf("email"))),
        )

        assertEquals("EMAIL", remote.signIn("a@b.dev", "Password123").account.authProvider)
    }

    @Test
    fun `a pending invite decision is the only one that holds the user back`() = runTest {
        server.next(Paths.LOGIN, body = Server.token(user = Server.user(inviteDecision = "pending")))
        assertTrue(remote.signIn("a@b.dev", "Password123").invitePending)

        server.next(Paths.LOGIN, body = Server.token(user = Server.user(inviteDecision = "applied")))
        assertFalse(remote.signIn("a@b.dev", "Password123").invitePending)

        server.next(Paths.LOGIN, body = Server.token(user = Server.user(inviteDecision = "skipped")))
        assertFalse(remote.signIn("a@b.dev", "Password123").invitePending)
    }

    @Test
    fun `restoring never issues a token`() = runTest {
        server.next(Paths.ME, body = Server.user(inviteDecision = "pending"))

        val session = remote.restore()

        assertNull(tokenStore.token)
        assertTrue(session.invitePending)
    }

    @Test
    fun `the flags are read off the current user`() = runTest {
        server.next(Paths.ME, body = Server.user(giftPopupSeen = true, bonusReservedIon = 1_500L))

        val flags = remote.flags()

        assertTrue(flags.bonusPopupShown)
        assertEquals(1_500L, flags.reservedBonusIon)
    }

    @Test
    fun `an untouched popup is reported as not shown`() = runTest {
        server.next(Paths.ME, body = Server.user(giftPopupSeen = false))

        assertFalse(remote.flags().bonusPopupShown)
    }

    @Test
    fun `applying a code names it as a manual entry`() = runTest {
        server.next(Paths.INVITE_APPLY, body = Server.inviteState())

        remote.applyInviteCode("SYREX482")

        assertEquals("""{"code":"SYREX482","source":"manual"}""", server.bodyOf(Paths.INVITE_APPLY))
    }

    @Test
    fun `skipping and marking the popup carry no body at all`() = runTest {
        server.next(Paths.INVITE_SKIP, body = Server.inviteState(decision = "skipped"))
        server.next(Paths.GIFT_POPUP_SEEN, body = Server.giftPopupSeen())

        remote.skipInvite()
        remote.markBonusPopupShown()

        assertEquals("", server.bodyOf(Paths.INVITE_SKIP))
        assertEquals("", server.bodyOf(Paths.GIFT_POPUP_SEEN))
    }

    @Test
    fun `a password reset posts the email`() = runTest {
        server.next(Paths.PASSWORD_RESET, body = """{"message":"sent"}""")

        remote.requestPasswordReset("said@ioaura.app")

        assertEquals("""{"email":"said@ioaura.app"}""", server.bodyOf(Paths.PASSWORD_RESET))
    }

    @Test
    fun `booting refreshes the config and reads the mesh`() = runTest {
        server.always(Paths.CONFIG, body = Bodies.CONFIG)
        server.next(Paths.MESH, body = Server.mesh(nodesOnline = 12_048))

        assertEquals(12_048, remote.bootstrap().nodeCount)
        assertEquals(1, server.hits(Paths.CONFIG))
    }

    @Test
    fun `a mesh that will not answer leaves the node count unknown`() = runTest {
        server.always(Paths.CONFIG, body = Bodies.CONFIG)
        server.next(Paths.MESH, code = 500, body = Server.detail("Internal Server Error"))

        assertNull(remote.bootstrap().nodeCount)
    }

    @Test
    fun `an empty mesh leaves the node count unknown`() = runTest {
        server.always(Paths.CONFIG, body = Bodies.CONFIG)
        server.next(Paths.MESH, body = Server.mesh(nodesOnline = 0))

        assertNull(remote.bootstrap().nodeCount)
    }

    @Test
    fun `booting survives a config the server will not give`() = runTest {
        server.always(Paths.CONFIG, code = 500, body = Server.detail("Internal Server Error"))
        server.next(Paths.MESH, body = Server.mesh(nodesOnline = 7))

        assertEquals(7, remote.bootstrap().nodeCount)
    }
}
