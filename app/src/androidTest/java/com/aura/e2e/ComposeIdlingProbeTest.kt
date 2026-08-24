package com.aura.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.aura.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposeIdlingProbeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun the_app_reaches_compose_idle_within_ten_seconds() {
        val reached = try {
            rule.waitUntil(10_000) {
                rule.onAllNodesWithText("I", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            true
        } catch (timedOut: androidx.compose.ui.test.ComposeTimeoutException) {
            false
        }

        assertTrue(
            "Compose never reported idle, so onNode/performClick cannot drive the app",
            reached,
        )
    }
}
