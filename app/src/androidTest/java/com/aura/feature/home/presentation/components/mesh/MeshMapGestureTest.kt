package com.aura.feature.home.presentation.components.mesh

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.GeoPoint
import com.aura.feature.home.domain.model.MeshCity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MeshMapGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var state: MeshMapState

    private fun setContent() {
        composeRule.setContent {
            AuraTheme {
                state = rememberMeshMapState()
                MeshMap(
                    cities = CITIES,
                    userPresence = null,
                    state = state,
                    modifier = Modifier
                        .size(width = 320.dp, height = 150.dp)
                        .testTag(MAP_TAG),
                )
            }
        }
    }

    @Test
    fun pinchZoomsTheMapIn() {
        setContent()

        composeRule.onNodeWithTag(MAP_TAG).performTouchInput {
            pinch(
                start0 = Offset(centerX - 20f, centerY),
                end0 = Offset(centerX - 130f, centerY),
                start1 = Offset(centerX + 20f, centerY),
                end1 = Offset(centerX + 130f, centerY),
            )
        }
        composeRule.waitForIdle()

        assertTrue("Ожидали увеличение, получили ${state.scale}", state.scale > 1f)
    }

    @Test
    fun dragPansTheMapOnlyWhenZoomedIn() {
        setContent()

        composeRule.onNodeWithTag(MAP_TAG).performTouchInput {
            swipe(start = Offset(centerX + 60f, centerY), end = Offset(centerX - 60f, centerY))
        }
        composeRule.waitForIdle()

        assertEquals("В полном виде карта не двигается", Offset.Zero, state.offset)

        composeRule.onNodeWithTag(MAP_TAG).performTouchInput {
            pinch(
                start0 = Offset(centerX - 20f, centerY),
                end0 = Offset(centerX - 130f, centerY),
                start1 = Offset(centerX + 20f, centerY),
                end1 = Offset(centerX + 130f, centerY),
            )
        }
        composeRule.onNodeWithTag(MAP_TAG).performTouchInput {
            swipe(start = Offset(centerX + 60f, centerY), end = Offset(centerX - 60f, centerY))
        }
        composeRule.waitForIdle()

        assertTrue("Приближенная карта должна перетаскиваться", state.offset != Offset.Zero)
    }

    private fun setScrollableContent(): ScrollState {
        val scrollState = ScrollState(0)
        composeRule.setContent {
            AuraTheme {
                state = rememberMeshMapState()
                Column(Modifier.verticalScroll(scrollState)) {
                    MeshMap(
                        cities = CITIES,
                        userPresence = null,
                        state = state,
                        modifier = Modifier
                            .size(width = 320.dp, height = 150.dp)
                            .testTag(MAP_TAG),
                    )
                    Spacer(Modifier.height(2_000.dp))
                }
            }
        }
        return scrollState
    }

    @Test
    fun dragOverTheWorldViewScrollsThePageUnderneath() {
        val scrollState = setScrollableContent()

        composeRule.onNodeWithTag(MAP_TAG).performTouchInput {
            swipe(start = Offset(centerX, centerY + 50f), end = Offset(centerX, centerY - 50f))
        }
        composeRule.waitForIdle()

        assertTrue("Страница должна прокручиваться пальцем по карте", scrollState.value > 0)
        assertEquals("Карта в полном виде не двигается", Offset.Zero, state.offset)
    }

    @Test
    fun dragOverTheZoomedMapLeavesThePageStill() {
        val scrollState = setScrollableContent()

        composeRule.onNodeWithTag(MAP_TAG).performTouchInput {
            pinch(
                start0 = Offset(centerX - 20f, centerY),
                end0 = Offset(centerX - 130f, centerY),
                start1 = Offset(centerX + 20f, centerY),
                end1 = Offset(centerX + 130f, centerY),
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MAP_TAG).performTouchInput {
            swipe(start = Offset(centerX, centerY + 50f), end = Offset(centerX, centerY - 50f))
        }
        composeRule.waitForIdle()

        assertEquals("Приближенная карта забирает жест себе", 0, scrollState.value)
        assertNotEquals(Offset.Zero, state.offset)
    }

    @Test
    fun doubleTapResetsToWorldView() {
        setContent()

        composeRule.onNodeWithTag(MAP_TAG).performTouchInput {
            pinch(
                start0 = Offset(centerX - 20f, centerY),
                end0 = Offset(centerX - 130f, centerY),
                start1 = Offset(centerX + 20f, centerY),
                end1 = Offset(centerX + 130f, centerY),
            )
        }
        composeRule.waitForIdle()
        assertTrue(state.scale > 1f)

        composeRule.onNodeWithTag(MAP_TAG).performTouchInput { doubleClick() }
        composeRule.waitUntil(timeoutMillis = 3_000) { !state.isZoomed }

        assertEquals(1f, state.scale, 0.01f)
        assertEquals(Offset.Zero, state.offset)
    }

    private companion object {
        const val MAP_TAG = "mesh-map"

        val CITIES = listOf(
            MeshCity("london", "London", GeoPoint(51.51, -0.13), isLive = true),
            MeshCity("tokyo", "Tokyo", GeoPoint(35.68, 139.65), isLive = false),
        )
    }
}
