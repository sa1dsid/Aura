package com.aura.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    private val aura = AuraDriver()

    @Before
    fun setUp() {
        aura.restart()
    }

    @Test
    fun cold_start_lands_on_the_auth_screen() {
        assertTrue(aura.has("In the beginning was the ION", LONG_WAIT))
        assertTrue(aura.has("Continue with Google"))
        assertTrue(aura.has("Forgot password"))
    }

    @Test
    fun a_new_account_walks_auth_to_invite_to_gift_to_home() {
        aura.signUp()

        assertTrue(aura.has("Got an invite code", LONG_WAIT))
        assertTrue(aura.click("have a code"))

        assertTrue(aura.has("3,000 ION are waiting for you", LONG_WAIT))
        assertTrue(aura.click("Later"))

        assertTrue(aura.has("ACCRUED", LONG_WAIT))
        assertTrue(aura.has("AVAILABLE TO WITHDRAW"))
        assertTrue(aura.has("NODE STATUS"))
    }

    @Test
    fun a_password_under_eight_characters_never_reaches_the_invite_screen() {
        aura.signUp(password = "1234567")

        assertFalse(aura.has("Got an invite code", SHORT_WAIT))
        assertTrue(aura.has("Create Account"))
    }

    @Test
    fun an_invite_code_of_the_wrong_length_keeps_the_user_on_the_invite_screen() {
        aura.signUp()
        assertTrue(aura.has("Got an invite code", LONG_WAIT))

        aura.type("SYREX482", "ABC12")
        aura.hideKeyboard()
        aura.click("Apply Code")

        assertTrue(aura.has("Got an invite code", SHORT_WAIT))
    }

    @Test
    fun the_gift_popup_closes_from_either_button() {
        aura.signUp()
        aura.skipInvite()

        assertTrue(aura.has("3,000 ION are waiting for you", LONG_WAIT))
        assertTrue(aura.click("Show me the steps"))

        assertTrue(aura.has("ACCRUED", LONG_WAIT))
    }

    @Test
    fun home_shows_the_main_button_and_the_five_tabs() {
        aura.reachHome()

        assertTrue(aura.scrollTo("TAP TO"))
        assertTrue(aura.has("+20 ION"))
        for (tab in listOf("HOME", "NODES", "IONI", "TERMINAL", "NETWORK")) {
            assertTrue(tab, aura.tabAt(tab) != null)
        }
    }

    @Test
    fun the_burger_menu_carries_every_row_the_layout_asks_for() {
        aura.reachHome()

        aura.scrollToTop()
        val menu = aura.device.findObjects(androidx.test.uiautomator.By.desc("Menu")).firstOrNull()
        assertTrue("burger", menu != null)
        menu!!.click()

        assertTrue(aura.has("Push notifications", LONG_WAIT))
        assertTrue(aura.has("Terms of Service"))
        assertTrue(aura.has("Privacy Policy"))
        assertTrue(aura.has("Log out"))
        assertTrue(aura.has("Delete account"))
        assertTrue(aura.has("IO Aura · v"))
        assertFalse(aura.has("Battery", 2_000))
        assertFalse(aura.has("Support", 2_000))
    }

    @Test
    fun the_delete_account_dialog_keeps_the_account_when_asked_to() {
        aura.reachHome()
        aura.scrollToTop()
        aura.device.findObjects(androidx.test.uiautomator.By.desc("Menu")).firstOrNull()?.click()
        assertTrue(aura.has("Delete account", LONG_WAIT))
        aura.click("Delete account")

        assertTrue(aura.has("Delete account?", LONG_WAIT))
        assertTrue(aura.has("This permanently deletes your account"))
        assertTrue(aura.has("Keep my account"))
        assertTrue(aura.has("Delete permanently"))

        aura.click("Keep my account")
        assertTrue(aura.has("Log out", SHORT_WAIT))
    }

    @Test
    fun the_news_drawer_opens_empty_and_names_the_way_out() {
        aura.reachHome()
        val planet = aura.newsPlanet()
        assertTrue("planet", planet != null)
        planet!!.click()

        assertTrue(aura.has("NEWS", LONG_WAIT))
        assertTrue(aura.has("tap a card to expand"))
    }

    @Test
    fun the_nodes_screen_shows_the_empty_state_and_the_personal_code() {
        aura.reachHome()
        aura.tabAt("NODES")!!.click()

        assertTrue(aura.has("Your network of invited friends", LONG_WAIT))
        assertTrue(aura.has("Your code"))
        assertTrue(aura.has("Share Invite Link"))
        assertTrue(aura.has("No friends yet"))
        assertTrue(aura.scrollTo("FIND YOUR NODE"))
        assertTrue("Snapchat", aura.scrollTo("Snapchat"))
        for (network in listOf("Reddit", "Instagram", "Snapchat")) {
            assertTrue(network, aura.node(network) != null)
        }
    }

    @Test
    fun the_terminal_hub_carries_exactly_two_cards() {
        aura.reachHome()
        aura.tabAt("TERMINAL")!!.click()

        assertTrue(aura.has("TRANSACTIONS", LONG_WAIT))
        assertTrue(aura.has("PROMO CODES"))
        assertFalse(aura.has("Data Share", 2_000))
        assertFalse(aura.has("Daily Limit", 2_000))
    }

    @Test
    fun transactions_opens_as_a_nested_screen_with_a_back_arrow() {
        aura.reachHome()
        aura.tabAt("TERMINAL")!!.click()
        assertTrue(aura.has("TRANSACTIONS", LONG_WAIT))
        aura.click("TRANSACTIONS")

        assertTrue(aura.has("Every ION and Spark event", LONG_WAIT))
        assertTrue(
            "back arrow",
            aura.device.findObjects(androidx.test.uiautomator.By.desc("Back")).isNotEmpty(),
        )

        aura.device.pressBack()
        assertTrue(aura.has("PROMO CODES", LONG_WAIT))
    }

    @Test
    fun the_network_screen_shows_the_diagnostics_cards() {
        aura.reachHome()
        aura.tabAt("NETWORK")!!.click()

        assertTrue(aura.has("Network", LONG_WAIT))
        assertTrue(aura.has("PING"))
        assertTrue(aura.has("JITTER"))
        assertTrue(aura.has("PACKET LOSS"))
        assertTrue(aura.has("IP ADDRESS"))
        assertTrue(aura.has("PROTOCOL"))
        assertTrue(aura.has("LOCATION"))
        assertTrue(aura.has("VPN"))
        assertTrue(aura.scrollTo("Ping History"))
    }
}
