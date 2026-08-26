package com.aura.core.api

import com.aura.core.api.dto.EmailCredentialsDto
import com.aura.core.api.dto.GoogleSignInRequestDto
import com.aura.core.api.dto.InviteApplyDto
import com.aura.core.api.dto.LocationUpdateDto
import com.aura.core.api.dto.NetworkStateUpdateDto
import com.aura.core.api.dto.PasswordResetConfirmDto
import com.aura.core.api.dto.PasswordResetRequestDto
import com.aura.core.api.dto.PingCreateDto
import com.aura.core.api.dto.PreferenceUpdateDto
import com.aura.core.api.dto.PushTokenDeleteDto
import com.aura.core.api.dto.PushTokenRegisterDto
import com.aura.core.api.dto.EarningStateUpdateDto
import com.aura.core.api.dto.TapFinishDto
import com.aura.core.api.dto.TapStartDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuraApiContractTest {

    private val server = ApiTestServer()

    private val api = server.api

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `register posts credentials and reads the token envelope`() = runTest {
        server.enqueue(201, Bodies.TOKEN)

        val token = api.register(EmailCredentialsDto("a@b.dev", "Password123"))
        val request = server.take()

        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/register", request.path)
        assertEquals("""{"email":"a@b.dev","password":"Password123"}""", request.body.readUtf8())
        assertEquals("header.payload.signature", token.accessToken)
        assertEquals(604800, token.expiresIn)
        assertTrue(token.isNewAccount)
        assertEquals("3ZBCZ9MA", token.user.promoCode)
        assertEquals(3000, token.user.bonusReservedIon)
        assertEquals("EE", token.user.country)
        assertEquals(listOf("email"), token.user.authMethods)
    }

    @Test
    fun `login reuses the register envelope`() = runTest {
        server.enqueue(200, Bodies.TOKEN)

        val token = api.login(EmailCredentialsDto("a@b.dev", "Password123"))

        assertEquals("/api/v1/auth/login", server.take().path)
        assertEquals("pending", token.user.inviteDecision)
    }

    @Test
    fun `google sign in sends the id token under its wire name`() = runTest {
        server.enqueue(200, Bodies.TOKEN)

        api.googleSignIn(GoogleSignInRequestDto("id-token-value"))

        val request = server.take()
        assertEquals("/api/v1/auth/google", request.path)
        assertEquals("""{"id_token":"id-token-value"}""", request.body.readUtf8())
    }

    @Test
    fun `password reset request and confirm keep their wire names`() = runTest {
        server.enqueue(200, """{"message":"If the account exists, a reset link was sent"}""")
        server.enqueue(200, """{"message":"Password updated"}""")

        val requested = api.requestPasswordReset(PasswordResetRequestDto("a@b.dev"))
        assertEquals("/api/v1/auth/password-reset/request", server.take().path)
        assertTrue(requested.message.isNotBlank())

        api.confirmPasswordReset(PasswordResetConfirmDto("tok", "Password123"))
        val confirm = server.take()
        assertEquals("/api/v1/auth/password-reset/confirm", confirm.path)
        assertEquals(
            """{"token":"tok","new_password":"Password123"}""",
            confirm.body.readUtf8(),
        )
    }

    @Test
    fun `current user reads every declared field`() = runTest {
        server.enqueue(200, Bodies.USER)

        val user = api.currentUser()

        assertEquals("GET", server.take().method)
        assertEquals(39, user.id)
        assertEquals("smoke", user.displayName)
        assertEquals("0.000000", user.sparkBalance)
        assertFalse(user.giftPopupSeen)
    }

    @Test
    fun `delete account tolerates an empty 204`() = runTest {
        server.enqueue(204)

        api.deleteCurrentUser()

        val request = server.take()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/auth/me", request.path)
    }

    @Test
    fun `invite state and apply share one response shape`() = runTest {
        server.enqueue(200, Bodies.INVITE_STATE)
        server.enqueue(200, Bodies.INVITE_STATE)
        server.enqueue(200, Bodies.INVITE_STATE)

        val state = api.inviteState()
        assertEquals("/api/v1/onboarding/invite", server.take().path)
        assertEquals("applied", state.decision)
        assertEquals("IDF46VS0", state.appliedCode)

        api.applyInvite(InviteApplyDto("IDF46VS0"))
        val apply = server.take()
        assertEquals("/api/v1/onboarding/invite/apply", apply.path)
        assertEquals("""{"code":"IDF46VS0","source":"manual"}""", apply.body.readUtf8())

        api.skipInvite()
        assertEquals("/api/v1/onboarding/invite/skip", server.take().path)
    }

    @Test
    fun `onboarding seen flags read their booleans`() = runTest {
        server.enqueue(200, """{"gift_popup_seen":true,"bonus_teaser_should_blink":true}""")
        server.enqueue(200, """{"bonus_teaser_seen":true}""")

        val gift = api.markGiftPopupSeen()
        assertEquals("/api/v1/onboarding/gift-popup/seen", server.take().path)
        assertTrue(gift.giftPopupSeen)
        assertTrue(gift.bonusTeaserShouldBlink)

        assertTrue(api.markBonusTeaserSeen().bonusTeaserSeen)
        assertEquals("/api/v1/onboarding/bonus-teaser/seen", server.take().path)
    }

    @Test
    fun `dashboard reads nested node bonus and battery blocks`() = runTest {
        server.enqueue(200, Bodies.DASHBOARD)

        val dashboard = api.dashboard()

        assertEquals("/api/v1/home/dashboard", server.take().path)
        assertEquals(20L, dashboard.accruedIon)
        assertEquals(3000L, dashboard.reservedBonusIon)
        assertEquals(1, dashboard.tapCount)
        assertEquals("2026-08-25T05:10:18.767918Z", dashboard.cooldownAvailableAt)
        assertEquals(6, dashboard.node.progressTarget)
        assertEquals(100, dashboard.node.rateMultiplierPercent)
        assertEquals(4, dashboard.bonus.total)
        assertTrue(dashboard.bonus.dataShareSoon)
        assertTrue(dashboard.batteryOptimization.shouldShow)
        assertTrue(dashboard.bonusTeaserShouldBlink)
        assertEquals(4, dashboard.sparkCoupon.limit)
    }

    @Test
    fun `dashboard spark balance survives the unrounded decimal the server sends`() = runTest {
        server.enqueue(200, Bodies.DASHBOARD)

        val dashboard = api.dashboard()

        assertEquals("17.12962966666666666666666667", dashboard.sparkBalance)
        assertEquals(17.129629666666666, dashboard.sparkBalance.toDouble(), 1e-9)
    }

    @Test
    fun `tap start sends the enum value and every anti fraud flag`() = runTest {
        server.enqueue(200, Bodies.TAP_RUNNING)

        val state = api.startTap(TapStartDto(networkType = "wifi", vpn = false, emulator = false))
        val request = server.take()

        assertEquals("/api/v1/home/tap/start", request.path)
        assertEquals(
            """{"network_type":"wifi","vpn":false,"emulator":false}""",
            request.body.readUtf8(),
        )
        assertEquals("running", state.status)
        assertEquals("f19b9aea-ef96-4972-ac64-12c4e297c7d0", state.sessionId)
        assertEquals(0, state.sparkWindowRate)
    }

    @Test
    fun `tap heartbeat and finish put the session id in the path`() = runTest {
        server.enqueue(200, Bodies.TAP_RUNNING)
        server.enqueue(200, Bodies.TAP_COMPLETED)

        api.tapHeartbeat("abc-123")
        assertEquals("/api/v1/home/tap/abc-123/heartbeat", server.take().path)

        val finished = api.finishTap("abc-123", TapFinishDto(interrupted = false))
        val request = server.take()
        assertEquals("/api/v1/home/tap/abc-123/finish", request.path)
        assertEquals(
            """{"interrupted":false,"network_lost":false,"app_backgrounded":false}""",
            request.body.readUtf8(),
        )
        assertEquals("completed", finished.status)
        assertEquals(20L, finished.accruedIon)
        assertEquals(20000, finished.sparkWindowRate)
    }

    @Test
    fun `integrity challenge reads hash and expiry`() = runTest {
        server.enqueue(
            200,
            """{"request_hash":"4521c9e0","expires_at":"2026-08-24T16:59:20.588150Z"}""",
        )

        val challenge = api.issueIntegrityChallenge()

        assertEquals("/api/v1/home/tap/integrity-challenge", server.take().path)
        assertEquals("4521c9e0", challenge.requestHash)
    }

    @Test
    fun `battery optimization endpoints share one response shape`() = runTest {
        val body = """{"should_show":true,"declined_at":null,"optimization_disabled":false}"""
        server.enqueue(200, body)
        server.enqueue(200, """{"should_show":false,"declined_at":"2026-08-24T17:00:00Z","optimization_disabled":false}""")
        server.enqueue(200, """{"should_show":false,"declined_at":null,"optimization_disabled":true}""")

        assertTrue(api.batteryOptimization().shouldShow)
        assertEquals("/api/v1/home/battery-optimization", server.take().path)

        val declined = api.declineBatteryOptimization()
        assertEquals("/api/v1/home/battery-optimization/decline", server.take().path)
        assertEquals("2026-08-24T17:00:00Z", declined.declinedAt)

        val disabled = api.confirmBatteryOptimizationDisabled()
        assertEquals(
            "/api/v1/home/battery-optimization/confirmed-disabled",
            server.take().path,
        )
        assertTrue(disabled.optimizationDisabled)
    }

    @Test
    fun `earning state keeps vpn in the body even when it is false`() = runTest {
        server.enqueue(200, """{"paused":false,"cooldown_available_at":null,"spark_balance":"0.000000"}""")

        val state = api.updateEarningState(EarningStateUpdateDto(vpn = false, emulator = false))
        val request = server.take()

        assertEquals("PUT", request.method)
        assertEquals("/api/v1/home/earning-state", request.path)
        assertEquals("""{"vpn":false,"emulator":false}""", request.body.readUtf8())
        assertFalse(state.paused)
    }

    @Test
    fun `home heartbeat mesh and location read their fields`() = runTest {
        server.enqueue(200, """{"nodes_online":1,"stale":false}""")
        server.enqueue(200, Bodies.MESH)
        server.enqueue(200, """{"city":"Tallinn"}""")

        val beat = api.heartbeat()
        assertEquals("/api/v1/home/heartbeat", server.take().path)
        assertEquals(1, beat.nodesOnline)
        assertFalse(beat.stale)

        val mesh = api.mesh()
        assertEquals("/api/v1/home/mesh", server.take().path)
        assertEquals(3, mesh.glowingCities.size)
        assertTrue(mesh.nodesOnlineStale)

        val city = api.updateLocation(LocationUpdateDto(vpn = false))
        val request = server.take()
        assertEquals("/api/v1/home/location", request.path)
        assertEquals("""{"vpn":false}""", request.body.readUtf8())
        assertEquals("Tallinn", city.city)
    }

    @Test
    fun `preferences patch sends push enabled`() = runTest {
        server.enqueue(200, """{"push_enabled":false}""")

        val updated = api.updatePreferences(PreferenceUpdateDto(pushEnabled = false))
        val request = server.take()

        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/home/preferences", request.path)
        assertEquals("""{"push_enabled":false}""", request.body.readUtf8())
        assertFalse(updated.pushEnabled)
    }

    @Test
    fun `push token delete carries a body on a DELETE`() = runTest {
        server.enqueue(200, """{"registered":true}""")
        server.enqueue(200, """{"removed":true}""")

        api.registerPushToken(PushTokenRegisterDto("fcm-token-0123456789"))
        val register = server.take()
        assertEquals("/api/v1/home/push-tokens", register.path)
        assertEquals(
            """{"token":"fcm-token-0123456789","platform":"android"}""",
            register.body.readUtf8(),
        )

        val removed = api.removePushToken(PushTokenDeleteDto("fcm-token-0123456789"))
        val delete = server.take()
        assertEquals("DELETE", delete.method)
        assertEquals("""{"token":"fcm-token-0123456789"}""", delete.body.readUtf8())
        assertTrue(removed.removed)
    }

    @Test
    fun `news list and open read the drawer contract`() = runTest {
        server.enqueue(
            200,
            """[{"id":1,"title":"Hi","body":"Text","published_at":"2026-08-24T10:00:00Z","is_read":false}]""",
        )
        server.enqueue(200, """{"unread":0}""")

        val news = api.news()
        assertEquals("/api/v1/home/news", server.take().path)
        assertEquals(1, news.size)
        assertFalse(news.first().isRead)

        assertEquals(0, api.openNews().unread)
        assertEquals("/api/v1/home/news/open", server.take().path)
    }

    @Test
    fun `public config reads flags and social links`() = runTest {
        server.enqueue(200, Bodies.CONFIG)

        val config = api.publicConfig()

        assertEquals("/api/v1/home/config", server.take().path)
        assertEquals("https://io-aura.example/terms", config.termsUrl)
        assertFalse(config.featureFlags.dataShare)
        assertEquals(6, config.socialLinks.size)
        assertTrue(config.socialLinks.all { it.url.isBlank() })
    }

    @Test
    fun `nodes reads tier progress and referral earnings`() = runTest {
        server.enqueue(200, Bodies.NODES)

        val nodes = api.nodes()

        assertEquals("/api/v1/nodes", server.take().path)
        assertEquals(0, nodes.friendsJoined)
        assertEquals(6, nodes.nextThreshold)
        assertNull(nodes.moreForNextTier)
        assertEquals("0.000000", nodes.earnedFromReferrals.spark)
        assertTrue(nodes.friends.isEmpty())
    }

    @Test
    fun `terminal counters and its two lists`() = runTest {
        server.enqueue(200, Bodies.TERMINAL)
        server.enqueue(200, Bodies.TRANSACTIONS)
        server.enqueue(
            200,
            """[{"id":3,"code":"SPARK123","campaign":"spark","created_at":"2026-08-24T10:00:00Z"}]""",
        )

        val terminal = api.terminal()
        assertEquals("/api/v1/terminal", server.take().path)
        assertEquals(0, terminal.transactionsNew)

        val transactions = api.transactions(limit = 100)
        assertEquals("/api/v1/terminal/transactions?limit=100", server.take().path)
        assertEquals("tap_reward", transactions.first().kind)
        assertEquals("ION", transactions.first().currency)
        assertEquals("20.000000", transactions.first().amount)

        val promos = api.promoCodes()
        assertEquals("/api/v1/terminal/promo-codes", server.take().path)
        assertEquals("SPARK123", promos.first().code)
    }

    @Test
    fun `network measurement post keeps every optional metric`() = runTest {
        server.enqueue(201, Bodies.PING)

        val ping = api.addMeasurement(
            PingCreateDto(
                source = "diagnostic",
                operator = "MegaFon",
                connection = "wifi",
                protocol = "IPv4",
                vpn = false,
                pingMs = 23.5,
                jitterMs = 3.1,
                packetLossPct = 0.0,
                downloadMbps = 55.2,
                uploadMbps = 12.3,
            )
        )
        val request = server.take()

        assertEquals("/api/v1/network/measurements", request.path)
        val sent = request.body.readUtf8()
        assertTrue(sent.contains(""""source":"diagnostic""""))
        assertTrue(sent.contains(""""vpn":false"""))
        assertTrue(sent.contains(""""ping_ms":23.5"""))
        assertEquals("23.500", ping.pingMs)
        assertEquals("3.6", ping.score)
    }

    @Test
    fun `network state and summary read the diagnostics cards`() = runTest {
        server.enqueue(200, Bodies.NETWORK_STATE)
        server.enqueue(200, Bodies.NETWORK_SUMMARY)
        server.enqueue(200, "[]")

        val state = api.updateNetworkState(
            NetworkStateUpdateDto("MegaFon", "wifi", "IPv4", vpn = false)
        )
        val request = server.take()
        assertEquals("PUT", request.method)
        assertEquals(
            """{"operator":"MegaFon","connection":"wifi","protocol":"IPv4","vpn":false}""",
            request.body.readUtf8(),
        )
        assertEquals("94.183.170.97", state.ip)

        val summary = api.networkSummary()
        assertEquals("/api/v1/network/summary", server.take().path)
        assertEquals("23.500", summary.pingMs)
        assertEquals("2026-08-24T16:57:20.470684Z", summary.lastTestedAt)

        assertTrue(api.measurements().isEmpty())
        assertEquals("/api/v1/network/measurements", server.take().path)
    }

    @Test
    fun `nullable server fields do not break parsing`() = runTest {
        server.enqueue(
            200,
            """{"ip":null,"operator":null,"location":null,"connection":null,"protocol":null,
            "vpn":null,"last_tested_at":null,"ping_ms":null,"jitter_ms":null,
            "packet_loss_pct":null}""",
        )

        val summary = api.networkSummary()

        assertNull(summary.ip)
        assertNull(summary.vpn)
        assertNull(summary.pingMs)
    }
}
