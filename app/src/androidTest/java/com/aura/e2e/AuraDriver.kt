package com.aura.e2e

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlin.random.Random

const val PKG = "com.aura"

const val PASSWORD = "Password123"

const val LONG_WAIT = 45_000L

const val SHORT_WAIT = 12_000L

private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"

class AuraDriver {

    val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun freshEmail(prefix: String): String {
        val tail = (1..10).map { ALPHABET[Random.nextInt(ALPHABET.length)] }.joinToString("")
        return "$prefix.$tail@auratest.dev"
    }

    fun launch() {
        val intent = context.packageManager.getLaunchIntentForPackage(PKG)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(PKG).depth(0)), LONG_WAIT)
    }

    fun restart() {
        launch()
        signOutIfSignedIn()
        check(has("In the beginning was the ION", LONG_WAIT)) {
            "could not get back to the auth screen before the test"
        }
    }

    fun appInForeground(): Boolean = device.hasObject(By.pkg(PKG).depth(0))

    fun onAuthScreen(): Boolean = device.findObject(By.textContains("beginning was the ION")) != null

    fun signOutIfSignedIn() {
        repeat(10) {
            if (!appInForeground()) {
                launch()
                device.waitForIdle(3_000)
                return@repeat
            }
            if (onAuthScreen()) return
            when {
                node("DENY") != null -> click("DENY", 3_000)
                node("Keep my account") != null -> click("Keep my account", 3_000)
                node("waiting for you") != null -> click("Later", 3_000)
                node("Got an invite code") != null -> skipInvite()
                node("Log out") != null -> click("Log out", 3_000)
                else -> {
                    scrollToTop()
                    device.findObjects(By.desc("Menu")).firstOrNull()?.click()
                }
            }
            device.waitForIdle(3_000)
        }
    }

    fun await(text: String, timeout: Long = LONG_WAIT): UiObject2? =
        device.wait(Until.findObject(By.textContains(text)), timeout)

    fun has(text: String, timeout: Long = SHORT_WAIT): Boolean = await(text, timeout) != null

    fun node(text: String): UiObject2? = device.findObject(By.textContains(text))

    fun count(text: String): Int = device.findObjects(By.textContains(text)).size

    fun click(text: String, timeout: Long = LONG_WAIT): Boolean {
        val target = await(text, timeout) ?: return false
        val point = target.visibleCenter
        device.click(point.x, point.y)
        return true
    }

    fun clickBurst(text: String, times: Int): Boolean {
        val target = await(text) ?: return false
        val point = target.visibleCenter
        repeat(times) { device.click(point.x, point.y) }
        return true
    }

    fun type(target: String, value: String) {
        val field = await(target) ?: return
        field.click()
        device.waitForIdle(2_000)
        device.executeShellCommand("input text $value")
    }

    fun hideKeyboard() {
        device.executeShellCommand("input keyevent 111")
        device.waitForIdle(1_500)
        if (!keyboardShown()) return
        device.pressBack()
        device.waitForIdle(1_500)
        if (!appInForeground()) launch()
    }

    fun keyboardShown(): Boolean =
        device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")

    fun scrollTo(text: String, tries: Int = 8): Boolean {
        repeat(tries) {
            if (node(text) != null) return true
            val h = device.displayHeight
            device.swipe(device.displayWidth / 2, (h * 0.72).toInt(), device.displayWidth / 2, (h * 0.30).toInt(), 12)
            device.waitForIdle(1_500)
        }
        return node(text) != null
    }

    fun signUp(email: String = freshEmail("e2e"), password: String = PASSWORD): String {
        click("Sign Up")
        device.waitForIdle(2_000)
        type("you@example.com", email)
        hideKeyboard()
        type("••••••••", password)
        hideKeyboard()
        click("Create Account")
        return email
    }

    fun signIn(email: String, password: String = PASSWORD) {
        val tab = device.findObjects(By.textContains("Sign In")).firstOrNull() ?: return
        val point = tab.visibleCenter
        device.click(point.x, point.y)
        device.waitForIdle(2_000)
        type("you@example.com", email)
        hideKeyboard()
        type("••••••••", password)
        hideKeyboard()
        device.findObjects(By.textContains("Sign In")).lastOrNull()?.let {
            val submit = it.visibleCenter
            device.click(submit.x, submit.y)
        }
    }

    fun skipInvite(): Boolean = click("have a code")

    fun closeGiftPopup(): Boolean = if (has("waiting for you", SHORT_WAIT)) click("Later") else false

    fun reachHome(email: String = freshEmail("e2e")): String {
        val used = signUp(email)
        skipInvite()
        closeGiftPopup()
        await("ACCRUED")
        return used
    }

    fun scrollToTop() {
        repeat(6) {
            if (device.findObjects(By.desc("Menu")).isNotEmpty()) return
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.30).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.75).toInt(),
                12,
            )
            device.waitForIdle(1_500)
        }
    }

    fun newsPlanet(): UiObject2? {
        scrollToTop()
        val menu = device.findObjects(By.desc("Menu")).firstOrNull() ?: return null
        return device.findObjects(By.clickable(true))
            .filter { it.visibleCenter.y in (menu.visibleCenter.y - 40)..(menu.visibleCenter.y + 40) }
            .maxByOrNull { it.visibleCenter.x }
            ?.takeIf { it.visibleCenter.x > device.displayWidth / 2 }
    }

    fun tabAt(label: String): UiObject2? =
        device.findObjects(By.textContains(label))
            .lastOrNull { it.visibleCenter.y > device.displayHeight * 0.85 }
}
