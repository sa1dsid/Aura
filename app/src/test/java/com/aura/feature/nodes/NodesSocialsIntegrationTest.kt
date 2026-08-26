package com.aura.feature.nodes

import com.aura.feature.nodes.domain.model.SocialNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SERVER_ERROR = 503

private val DESIGN_ORDER = listOf(
    SocialNetwork.DISCORD,
    SocialNetwork.TELEGRAM,
    SocialNetwork.X,
    SocialNetwork.REDDIT,
    SocialNetwork.INSTAGRAM,
    SocialNetwork.SNAPCHAT,
)

class NodesSocialsIntegrationTest : NodesTestCase() {

    @Test
    fun `all six networks reach the screen in the order of the design`() = nodes { stack ->
        stack.server.always(NodesPaths.CONFIG, body = Nodes.config(Nodes.allSocials()))
        val (_, states) = screenOf(stack)

        val socials = awaitContent(states).nodes.socials

        assertEquals(DESIGN_ORDER, socials.map { it.network })
    }

    @Test
    fun `the urls come from the server config`() = nodes { stack ->
        stack.server.always(NodesPaths.CONFIG, body = Nodes.config(Nodes.allSocials()))
        val (_, states) = screenOf(stack)

        val socials = awaitContent(states).nodes.socials

        assertEquals(
            Nodes.allSocials().map { (_, url) -> url }.sorted(),
            socials.map { it.webUrl }.sorted(),
        )
        assertTrue(socials.all { it.isOpenable })
    }

    @Test
    fun `a network the config leaves out stays closed`() = nodes { stack ->
        stack.server.always(
            NodesPaths.CONFIG,
            body = Nodes.config(listOf("discord" to "https://discord.gg/ioaura")),
        )
        val (_, states) = screenOf(stack)

        val socials = awaitContent(states).nodes.socials

        assertEquals(DESIGN_ORDER.size, socials.size)
        assertTrue(socials.single { it.network == SocialNetwork.DISCORD }.isOpenable)
        assertTrue(socials.filter { it.network != SocialNetwork.DISCORD }.none { it.isOpenable })
    }

    @Test
    fun `a blank url in the config counts as missing`() = nodes { stack ->
        stack.server.always(
            NodesPaths.CONFIG,
            body = Nodes.config(listOf("discord" to "", "telegram" to "https://t.me/ioaura")),
        )
        val (_, states) = screenOf(stack)

        val socials = awaitContent(states).nodes.socials

        assertFalse(socials.single { it.network == SocialNetwork.DISCORD }.isOpenable)
        assertTrue(socials.single { it.network == SocialNetwork.TELEGRAM }.isOpenable)
    }

    @Test
    fun `networks the screen cannot draw never reach it`() = nodes { stack ->
        stack.server.always(
            NodesPaths.CONFIG,
            body = Nodes.config(
                listOf(
                    "youtube" to "https://youtube.com/@ioaura",
                    "tiktok" to "https://tiktok.com/@ioaura",
                    "discord" to "https://discord.gg/ioaura",
                )
            ),
        )
        val (_, states) = screenOf(stack)

        assertEquals(DESIGN_ORDER, awaitContent(states).nodes.socials.map { it.network })
    }

    @Test
    fun `no social row ever carries an app link`() = nodes { stack ->
        stack.server.always(NodesPaths.CONFIG, body = Nodes.config(Nodes.allSocials()))
        val (_, states) = screenOf(stack)

        awaitContent(states).nodes.socials.forEach { assertNull(it.appUrl) }
    }

    @Test
    fun `the config is read once and later changes never arrive`() = nodes { stack ->
        val (viewModel, states) = screenOf(stack)
        awaitContent(states)

        stack.server.always(NodesPaths.CONFIG, body = Nodes.config(Nodes.allSocials()))
        viewModel.onScreenResumed()
        awaitRequest(stack, NodesPaths.NODES, count = 2)

        assertTrue(awaitContent(states).nodes.socials.none { it.isOpenable })
    }

    @Test
    fun `a failing config is asked again on the next refresh`() = nodes { stack ->
        stack.server.always(NodesPaths.CONFIG, code = SERVER_ERROR, body = "{}")
        val (viewModel, states) = screenOf(stack)
        assertTrue(awaitContent(states).nodes.socials.none { it.isOpenable })

        stack.server.always(NodesPaths.CONFIG, body = Nodes.config(Nodes.allSocials()))
        viewModel.onScreenResumed()

        awaitNodes(states, "the social links") { state -> state.socials.all { it.isOpenable } }
    }
}
