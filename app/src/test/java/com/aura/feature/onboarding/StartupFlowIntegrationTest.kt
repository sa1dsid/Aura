package com.aura.feature.onboarding

import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.StartDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupFlowIntegrationTest : OnboardingTestCase() {

    @Test
    fun `a cold start without a token never asks the server who is signed in`() =
        onboarding { stack ->
            assertEquals(StartDestination.AUTH, stack.resolveStartDestination())
            assertEquals(0, stack.server.hits(Paths.ME))
            assertNull(stack.sessionStore.account.value)
        }

    @Test
    fun `a live session with a settled invite opens home`() = onboarding { stack ->
        stack.signedInWithToken()
        stack.server.always(Paths.ME, body = Server.user(inviteDecision = Server.APPLIED))

        assertEquals(StartDestination.HOME, stack.resolveStartDestination())
    }

    @Test
    fun `a live session that skipped the invite opens home`() = onboarding { stack ->
        stack.signedInWithToken()
        stack.server.always(Paths.ME, body = Server.user(inviteDecision = Server.SKIPPED))

        assertEquals(StartDestination.HOME, stack.resolveStartDestination())
    }

    @Test
    fun `a live session that never answered the invite lands back on the invite screen`() =
        onboarding { stack ->
            stack.signedInWithToken()
            stack.server.always(Paths.ME, body = Server.user(inviteDecision = Server.PENDING))

            assertEquals(StartDestination.INVITE, stack.resolveStartDestination())
        }

    @Test
    fun `an expired session drops the token and opens auth`() = onboarding { stack ->
        stack.signedInWithToken()
        stack.server.always(Paths.ME, code = 401, body = Server.detail("Not authenticated"))

        assertEquals(StartDestination.AUTH, stack.resolveStartDestination())
        assertNull(stack.savedToken)
        assertNull(stack.sessionStore.account.value)
    }

    @Test
    fun `a session the server could not confirm never lands on home`() = onboarding { stack ->
        stack.signedInWithToken()
        stack.server.always(Paths.ME, code = 500, body = Server.detail("Internal Server Error"))

        assertEquals(StartDestination.AUTH, stack.resolveStartDestination())
        assertEquals("restored.session.token", stack.savedToken)
        assertNull(stack.sessionStore.account.value)
    }

    @Test
    fun `a dead network keeps the token but still opens auth`() = onboarding { stack ->
        stack.signedInWithToken()
        stack.server.nextDropsConnection(Paths.ME)

        assertEquals(StartDestination.AUTH, stack.resolveStartDestination())
        assertEquals("restored.session.token", stack.savedToken)
        assertNull(stack.sessionStore.account.value)
    }

    @Test
    fun `restoring a session opens it and refreshes the push subscription`() = onboarding { stack ->
        stack.signedInWithToken()
        stack.server.always(
            Paths.ME,
            body = Server.user(
                id = 77,
                email = "said@ioaura.app",
                displayName = "said",
                authMethods = listOf("email", "google"),
                promoCode = "SYREX482",
                inviteDecision = Server.APPLIED,
            ),
        )

        stack.resolveStartDestination()
        val account = stack.sessionStore.account.value

        assertEquals("77", account?.id)
        assertEquals("said@ioaura.app", account?.email)
        assertEquals("said", account?.handle)
        assertEquals("https://ioaura.app/i/SYREX482", account?.inviteLink)
        assertEquals(AuthProvider.GOOGLE, account?.authProvider)
        assertEquals(1, stack.pushRefreshes)
    }

    @Test
    fun `the boot config carries the live node count`() = onboarding { stack ->
        stack.server.always(
            Paths.MESH,
            body = Server.mesh(nodesOnline = 12_048, cities = listOf("Tallinn", "Rostov-na-Donu")),
        )

        assertEquals(BootConfig(nodeCount = 12_048), stack.bootstrap())
    }

    @Test
    fun `a mesh outage falls back to the built in node count`() = onboarding { stack ->
        stack.server.always(Paths.MESH, code = 500, body = Server.detail("Internal Server Error"))

        assertEquals(BootConfig.FALLBACK, stack.bootstrap())
    }

    @Test
    fun `an empty mesh is not shown as zero nodes online`() = onboarding { stack ->
        stack.server.always(Paths.MESH, body = Server.mesh(nodesOnline = 0, cities = emptyList()))

        assertEquals(BootConfig.DEFAULT_NODE_COUNT, stack.bootstrap().nodeCount)
    }

    @Test
    fun `the public config is fetched once no matter how often the app boots`() =
        onboarding { stack ->
            stack.bootstrap()
            stack.bootstrap()

            assertEquals(1, stack.server.hits(Paths.CONFIG))
            assertEquals(2, stack.server.hits(Paths.MESH))
        }

    @Test
    fun `booting does not need a session`() = onboarding { stack ->
        stack.bootstrap()

        assertEquals(0, stack.server.hits(Paths.ME))
    }
}
