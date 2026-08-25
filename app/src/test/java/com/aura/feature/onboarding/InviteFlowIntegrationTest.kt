package com.aura.feature.onboarding

import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.InviteFailure
import com.aura.feature.onboarding.presentation.invite.InviteEvent
import com.aura.feature.onboarding.presentation.invite.InviteViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val CODE = "SYREX482"

class InviteFlowIntegrationTest : OnboardingTestCase() {

    @Test
    fun `applying a code posts it as a manual entry and opens the bonus popup`() =
        onboarding { stack ->
            stack.openSession()
            stack.server.next(Paths.INVITE_APPLY, body = Server.inviteState())
            stack.server.always(Paths.ME, body = Server.user(giftPopupSeen = false))
            val viewModel = stack.inviteViewModel()
            val events = stack.eventsOf(viewModel.events)
            viewModel.onScreenResumed()

            viewModel.onCodeChange(CODE)
            viewModel.onApplyClick()
            awaitEvent(events)

            assertEquals(
                """{"code":"$CODE","source":"manual"}""",
                stack.server.bodyOf(Paths.INVITE_APPLY),
            )
            assertEquals(listOf(InviteEvent.Finished(bonusPopupPending = true)), events)
        }

    @Test
    fun `a code already seen by the bonus popup goes straight home`() = onboarding { stack ->
        stack.openSession()
        stack.server.next(Paths.INVITE_APPLY, body = Server.inviteState())
        stack.server.always(Paths.ME, body = Server.user(giftPopupSeen = true))
        val viewModel = stack.inviteViewModel()
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()

        viewModel.onCodeChange(CODE)
        viewModel.onApplyClick()
        awaitEvent(events)

        assertEquals(listOf(InviteEvent.Finished(bonusPopupPending = false)), events)
    }

    @Test
    fun `a code typed in lower case reaches the server upper cased`() = onboarding { stack ->
        stack.openSession()
        stack.server.next(Paths.INVITE_APPLY, body = Server.inviteState())
        stack.server.always(Paths.ME, body = Server.user())
        val viewModel = stack.inviteViewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange("syrex482")
        viewModel.onApplyClick()
        awaitRequest(stack, Paths.INVITE_APPLY)

        assertEquals(
            """{"code":"$CODE","source":"manual"}""",
            stack.server.bodyOf(Paths.INVITE_APPLY),
        )
    }

    @Test
    fun `an unknown code keeps the user on the screen`() = onboarding { stack ->
        stack.openSession()
        stack.server.next(Paths.INVITE_APPLY, code = 404, body = Server.detail("Invite not found"))
        val viewModel = stack.inviteViewModel()
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()

        viewModel.onCodeChange(CODE)
        viewModel.onApplyClick()
        awaitFailure(viewModel)

        assertTrue(events.isEmpty())
        assertEquals(InviteFailure.UNKNOWN_CODE, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.submitting)
        assertEquals(CODE, viewModel.uiState.value.code)
    }

    @Test
    fun `the own code of the account is told apart from a malformed one`() = onboarding { stack ->
        stack.openSession()
        stack.server.next(
            Paths.INVITE_APPLY,
            code = 422,
            body = Server.detail("Own invite code is not allowed"),
        )
        val viewModel = stack.inviteViewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange(CODE)
        viewModel.onApplyClick()
        awaitFailure(viewModel)

        assertEquals(InviteFailure.OWN_CODE, viewModel.uiState.value.failure)
    }

    @Test
    fun `a code the server cannot parse reads as unknown`() = onboarding { stack ->
        stack.openSession()
        stack.server.next(
            Paths.INVITE_APPLY,
            code = 422,
            body = Server.fieldError(field = "code", type = "string_pattern_mismatch"),
        )
        val viewModel = stack.inviteViewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange(CODE)
        viewModel.onApplyClick()
        awaitFailure(viewModel)

        assertEquals(InviteFailure.UNKNOWN_CODE, viewModel.uiState.value.failure)
    }

    @Test
    fun `a code applied twice reads as already applied`() = onboarding { stack ->
        stack.openSession()
        stack.server.next(
            Paths.INVITE_APPLY,
            code = 409,
            body = Server.detail("Invite already applied"),
        )
        val viewModel = stack.inviteViewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange(CODE)
        viewModel.onApplyClick()
        awaitFailure(viewModel)

        assertEquals(InviteFailure.ALREADY_APPLIED, viewModel.uiState.value.failure)
    }

    @Test
    fun `a dead network on apply reads as a connection problem`() = onboarding { stack ->
        stack.openSession()
        stack.server.nextDropsConnection(Paths.INVITE_APPLY)
        val viewModel = stack.inviteViewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange(CODE)
        viewModel.onApplyClick()
        awaitFailure(viewModel)

        assertEquals(InviteFailure.NETWORK, viewModel.uiState.value.failure)
    }

    @Test
    fun `a code shorter than eight characters never reaches the server`() = onboarding { stack ->
        stack.openSession()
        val viewModel = stack.inviteViewModel()
        viewModel.onScreenResumed()

        viewModel.onCodeChange("ABC12")
        viewModel.onApplyClick()

        assertEquals(0, stack.server.hits(Paths.INVITE_APPLY))
    }

    @Test
    fun `skipping posts the refusal and opens the bonus popup`() = onboarding { stack ->
        stack.openSession()
        stack.server.next(Paths.INVITE_SKIP, body = Server.inviteState(decision = Server.SKIPPED))
        stack.server.always(Paths.ME, body = Server.user(giftPopupSeen = false))
        val viewModel = stack.inviteViewModel()
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()

        viewModel.onSkipClick()
        awaitEvent(events)

        assertEquals(1, stack.server.hits(Paths.INVITE_SKIP))
        assertEquals(listOf(InviteEvent.Finished(bonusPopupPending = true)), events)
    }

    @Test
    fun `a code that came from a link arrives filled in and locked`() = onboarding { stack ->
        stack.openSession()
        stack.inviteRepository.rememberDeepLinkCode("syrex482")
        val viewModel = stack.inviteViewModel()

        viewModel.onScreenResumed()
        viewModel.onCodeChange("OTHER123")

        assertEquals(CODE, viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.locked)
    }

    @Test
    fun `skipping releases the code that came from a link`() = onboarding { stack ->
        stack.openSession()
        stack.inviteRepository.rememberDeepLinkCode(CODE)
        stack.server.next(Paths.INVITE_SKIP, body = Server.inviteState(decision = Server.SKIPPED))
        stack.server.always(Paths.ME, body = Server.user())
        val viewModel = stack.inviteViewModel()
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()

        viewModel.onSkipClick()
        awaitEvent(events)

        assertEquals(InviteAttribution.None, stack.inviteRepository.pendingAttribution())
    }

    @Test
    fun `applying releases the code that came from a link`() = onboarding { stack ->
        stack.openSession()
        stack.inviteRepository.rememberDeepLinkCode(CODE)
        stack.server.next(Paths.INVITE_APPLY, body = Server.inviteState())
        stack.server.always(Paths.ME, body = Server.user())
        val viewModel = stack.inviteViewModel()
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()

        viewModel.onApplyClick()
        awaitEvent(events)

        assertEquals(InviteAttribution.None, stack.inviteRepository.pendingAttribution())
    }

    @Test
    fun `a code carried by the install referrer arrives filled in and locked`() =
        onboarding(referrerCode = "syrex482") { stack ->
            stack.openSession()
            val viewModel = stack.inviteViewModel()

            viewModel.onScreenResumed()

            assertEquals(CODE, viewModel.uiState.value.code)
            assertTrue(viewModel.uiState.value.locked)
        }

    @Test
    fun `a code from a link wins over the one from the install referrer`() =
        onboarding(referrerCode = "AAAA1111") { stack ->
            stack.openSession()
            stack.inviteRepository.rememberDeepLinkCode("BBBB2222")
            val viewModel = stack.inviteViewModel()

            viewModel.onScreenResumed()

            assertEquals("BBBB2222", viewModel.uiState.value.code)
        }

    @Test
    fun `an unreachable flags endpoint still opens the bonus popup`() = onboarding { stack ->
        stack.openSession()
        stack.server.next(Paths.INVITE_SKIP, body = Server.inviteState(decision = Server.SKIPPED))
        stack.server.always(Paths.ME, code = 500, body = Server.detail("Internal Server Error"))
        val viewModel = stack.inviteViewModel()
        val events = stack.eventsOf(viewModel.events)
        viewModel.onScreenResumed()

        viewModel.onSkipClick()
        awaitEvent(events)

        assertEquals(listOf(InviteEvent.Finished(bonusPopupPending = true)), events)
    }

    @Test
    fun `the invite screen does nothing at all without a session`() = onboarding { stack ->
        val viewModel = stack.inviteViewModel()
        val events = stack.eventsOf(viewModel.events)

        viewModel.onScreenResumed()
        viewModel.onCodeChange(CODE)
        viewModel.onApplyClick()
        viewModel.onSkipClick()

        assertTrue(events.isEmpty())
        assertEquals(0, stack.server.hits(Paths.INVITE_APPLY))
        assertEquals(0, stack.server.hits(Paths.INVITE_SKIP))
    }

    @Test
    fun `the bonus popup reads the reserved amount of the signed in account`() =
        onboarding { stack ->
            stack.openSession()
            stack.server.always(Paths.ME, body = Server.user(bonusReservedIon = 5_000L))
            val viewModel = stack.bonusViewModel()

            viewModel.onScreenResumed()
            awaitUntil("the reserved bonus") { viewModel.bonusIon.value == 5_000L }

            assertEquals(5_000L, viewModel.bonusIon.value)
        }

    @Test
    fun `an unreachable server leaves the bonus popup showing three thousand`() =
        onboarding { stack ->
            stack.openSession()
            stack.server.always(Paths.ME, code = 500, body = Server.detail("Internal Server Error"))
            val viewModel = stack.bonusViewModel()

            viewModel.onScreenResumed()
            awaitRequest(stack, Paths.ME)

            assertEquals(3_000L, viewModel.bonusIon.value)
        }

    @Test
    fun `closing the bonus popup tells the server it was seen`() = onboarding { stack ->
        stack.openSession()
        stack.server.always(Paths.GIFT_POPUP_SEEN, body = Server.giftPopupSeen())
        val viewModel = stack.bonusViewModel()
        val dismissals = stack.eventsOf(viewModel.dismissed)

        viewModel.onDismiss()
        awaitEvent(dismissals)
        awaitRequest(stack, Paths.GIFT_POPUP_SEEN)

        assertEquals(1, dismissals.size)
        assertEquals(1, stack.server.hits(Paths.GIFT_POPUP_SEEN))
    }

    @Test
    fun `the bonus popup closes even when the server refuses the flag`() = onboarding { stack ->
        stack.openSession()
        stack.server.always(
            Paths.GIFT_POPUP_SEEN,
            code = 500,
            body = Server.detail("Internal Server Error"),
        )
        val viewModel = stack.bonusViewModel()
        val dismissals = stack.eventsOf(viewModel.dismissed)

        viewModel.onDismiss()
        awaitEvent(dismissals)

        assertEquals(1, dismissals.size)
    }

    @Test
    fun `the bonus popup never talks to the server without a session`() = onboarding { stack ->
        val viewModel = stack.bonusViewModel()

        viewModel.onScreenResumed()
        viewModel.onDismiss()

        assertNull(stack.sessionStore.account.value)
        assertEquals(0, stack.server.hits(Paths.GIFT_POPUP_SEEN))
    }

    private fun awaitFailure(viewModel: InviteViewModel) =
        awaitUntil("the invite failure") { viewModel.uiState.value.failure != null }

    private fun awaitRequest(stack: OnboardingStack, path: String) =
        awaitUntil("a request to $path") { stack.server.hits(path) >= 1 }
}
