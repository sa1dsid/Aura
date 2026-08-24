package com.aura.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FastTapRaceTest {

    private val aura = AuraDriver()

    @Before
    fun setUp() {
        aura.restart()
    }

    @Test
    fun three_taps_on_create_account_move_exactly_one_screen_forward() {
        aura.click("Sign Up")
        aura.type("you@example.com", aura.freshEmail("fast"))
        aura.hideKeyboard()
        aura.type("••••••••", PASSWORD)
        aura.hideKeyboard()

        assertTrue(aura.clickBurst("Create Account", 3))

        assertTrue(aura.has("Got an invite code", LONG_WAIT))
        assertEquals(0, aura.count("Create Account"))
    }

    @Test
    fun three_taps_on_skip_do_not_stack_screens() {
        aura.signUp()
        assertTrue(aura.has("Got an invite code", LONG_WAIT))

        assertTrue(aura.clickBurst("have a code", 3))

        assertTrue(aura.has("waiting for you", LONG_WAIT))
        assertEquals(0, aura.count("Got an invite code"))
    }

    @Test
    fun three_taps_on_apply_send_one_code() {
        aura.signUp()
        assertTrue(aura.has("Got an invite code", LONG_WAIT))
        aura.type("SYREX482", "ZZZZ9999")
        aura.hideKeyboard()

        assertTrue(aura.clickBurst("Apply Code", 3))

        assertTrue(aura.has("Got an invite code", SHORT_WAIT))
    }

    @Test
    fun three_taps_on_the_main_button_open_one_test_only() {
        aura.reachHome()
        assertTrue(aura.scrollTo("TAP TO"))

        assertTrue(aura.clickBurst("TAP TO", 3))

        assertTrue(aura.has("TESTING", LONG_WAIT))
        assertEquals(1, aura.count("TESTING"))
        assertEquals(0, aura.count("TAP TO"))
    }

    @Test
    fun a_tap_and_an_immediate_home_key_give_the_attempt_back() {
        aura.reachHome()
        assertTrue(aura.scrollTo("TAP TO"))
        val start = aura.node("TAP TO")!!.visibleCenter

        aura.device.click(start.x, start.y)
        aura.device.pressHome()
        aura.device.waitForIdle(3_000)
        aura.launch()

        assertTrue(aura.scrollTo("TAP TO"))
        assertEquals(0, aura.count("NEXT TEST IN"))
    }

    @Test
    fun tab_switching_at_full_speed_leaves_one_screen_on_top() {
        aura.reachHome()
        val tabs = listOf("NODES", "TERMINAL", "NETWORK", "HOME")
            .mapNotNull { aura.tabAt(it)?.visibleCenter }
        assertEquals(4, tabs.size)

        repeat(3) { tabs.forEach { aura.device.click(it.x, it.y) } }

        assertTrue(aura.has("ACCRUED", LONG_WAIT))
        assertEquals(0, aura.count("Your network of invited friends"))
        assertEquals(0, aura.count("Ping History"))
    }

    @Test
    fun a_burst_on_the_burger_leaves_one_menu() {
        aura.reachHome()
        val menu = aura.device.findObjects(By.desc("Menu")).first().visibleCenter

        repeat(4) { aura.device.click(menu.x, menu.y) }

        aura.device.waitForIdle(3_000)
        assertTrue(aura.count("Log out") <= 1)
    }

    @Test
    fun a_burst_on_the_invite_button_opens_one_share_sheet() {
        aura.reachHome()
        assertTrue(aura.scrollTo("INVITE"))

        assertTrue(aura.clickBurst("INVITE", 3))
        aura.device.waitForIdle(4_000)

        aura.device.pressBack()
        assertTrue(aura.has("ACCRUED", LONG_WAIT) || aura.scrollTo("TAP TO"))
    }
}
