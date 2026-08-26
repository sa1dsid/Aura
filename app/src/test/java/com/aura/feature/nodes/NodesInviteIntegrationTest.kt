package com.aura.feature.nodes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SERVER_ERROR = 503

class NodesInviteIntegrationTest : NodesTestCase() {

    @Test
    fun `the invite offer comes from the onboarding invite endpoint`() = nodes { stack ->
        val (_, states) = screenOf(stack)

        val invite = awaitContent(states).nodes.invite

        assertEquals(Nodes.PERSONAL_CODE, invite.code)
        assertEquals(Nodes.PERSONAL_URL, invite.link)
        assertEquals(Nodes.SHARE_TEXT, invite.shareText)
    }

    @Test
    fun `a failing invite endpoint falls back to the account link`() = nodes { stack ->
        stack.signIn(inviteLink = Nodes.ACCOUNT_LINK)
        stack.server.always(NodesPaths.INVITE, code = SERVER_ERROR, body = "{}")
        val (_, states) = screenOf(stack)

        val invite = awaitContent(states).nodes.invite

        assertEquals("", invite.code)
        assertEquals(Nodes.ACCOUNT_LINK, invite.link)
        assertNull(invite.shareText)
    }

    @Test
    fun `without a session and without an invite the offer stays empty`() =
        nodes(signedIn = false) { stack ->
            stack.server.always(NodesPaths.INVITE, code = SERVER_ERROR, body = "{}")
            val (_, states) = screenOf(stack)

            val invite = awaitContent(states).nodes.invite

            assertEquals("", invite.code)
            assertEquals("", invite.link)
            assertNull(invite.shareText)
        }

    @Test
    fun `an empty personal url wins over the account link`() = nodes { stack ->
        stack.signIn(inviteLink = Nodes.ACCOUNT_LINK)
        stack.server.always(NodesPaths.INVITE, body = Nodes.invite(personalUrl = ""))
        val (_, states) = screenOf(stack)

        assertEquals("", awaitContent(states).nodes.invite.link)
    }

    @Test
    fun `a blank share text leaves the screen to its own copy`() = nodes { stack ->
        stack.server.always(NodesPaths.INVITE, body = Nodes.invite(shareText = "   "))
        val (_, states) = screenOf(stack)

        assertNull(awaitContent(states).nodes.invite.shareText)
    }

    @Test
    fun `the invite quote never arrives from the server`() = nodes { stack ->
        val (_, states) = screenOf(stack)

        assertNull(awaitContent(states).nodes.invite.quote)
    }

    @Test
    fun `every refresh asks the invite endpoint again`() = nodes { stack ->
        val (viewModel, _) = screenOf(stack)
        awaitRequest(stack, NodesPaths.INVITE)

        viewModel.onScreenResumed()

        awaitRequest(stack, NodesPaths.INVITE, count = 2)
        assertTrue(stack.server.hits(NodesPaths.INVITE) >= 2)
    }

    @Test
    fun `a failing invite does not stop the snapshot from loading`() = nodes { stack ->
        stack.server.always(NodesPaths.INVITE, code = SERVER_ERROR, body = "{}")
        stack.server.always(
            NodesPaths.NODES,
            body = Nodes.snapshot(friendsJoined = 4, tier = Nodes.TIER_CORE_NODE),
        )
        val (_, states) = screenOf(stack)

        assertEquals(4, awaitContent(states).nodes.friendsJoined)
    }
}
