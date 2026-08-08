package com.aura.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_PRESS_MILLIS = 160L

private const val RELEASE_GAP_MILLIS = 45L

internal const val PRESS_FADE_MILLIS = 130

internal suspend fun InteractionSource.collectPressedState(
    minPressMillis: Long,
    releaseGapMillis: Long = RELEASE_GAP_MILLIS,
    currentTimeMillis: () -> Long = System::currentTimeMillis,
    onPressedChanged: (Boolean) -> Unit,
): Unit = coroutineScope {
    var releaseJob: Job? = null
    var isPressed = false
    var pressedAtMillis = 0L

    interactions.collect { interaction ->
        when (interaction) {
            is PressInteraction.Press -> {
                releaseJob?.cancelAndJoin()
                releaseJob = null

                if (isPressed) {
                    isPressed = false
                    onPressedChanged(false)
                    delay(releaseGapMillis)
                }

                isPressed = true
                pressedAtMillis = currentTimeMillis()
                onPressedChanged(true)
            }

            is PressInteraction.Release, is PressInteraction.Cancel -> {
                val pressStartedAtMillis = pressedAtMillis
                releaseJob = launch {
                    val held = currentTimeMillis() - pressStartedAtMillis
                    if (held < minPressMillis) delay(minPressMillis - held)
                    isPressed = false
                    onPressedChanged(false)
                }
            }
        }
    }
}

@Composable
fun rememberPressedState(
    interactionSource: InteractionSource,
    minPressMillis: Long = MIN_PRESS_MILLIS,
): State<Boolean> {
    val pressed = remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource, minPressMillis) {
        interactionSource.collectPressedState(minPressMillis) { pressed.value = it }
    }

    return pressed
}

@Composable
fun Modifier.pressScale(
    pressed: Boolean,
    enabled: Boolean = true,
    pressedScale: Float = 0.975f,
): Modifier {
    val isPressed = pressed && enabled
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 80)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "press-scale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.975f,
): Modifier {
    val pressed by rememberPressedState(interactionSource)
    return pressScale(pressed = pressed, enabled = enabled, pressedScale = pressedScale)
}
