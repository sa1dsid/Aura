package com.aura.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.theme.AuraTheme

private const val PLANET_BODY_FRACTION = 21.5f / 48f

private const val PULSE_MILLIS = 1400

private val PlanetSize = 48.dp

@Composable
fun AuraBurgerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.88f)
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_menu),
            contentDescription = stringResource(R.string.cd_menu),
            tint = AuraTheme.colors.textPrimary,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun AuraNewsPlanet(
    hasUnread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val pulse = rememberInfiniteTransition(label = "news-planet").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "news-pulse",
    )

    Box(
        modifier = modifier
            .size(PlanetSize)
            .auraGlow(Color.White.copy(alpha = 0.40f), width = 30.dp, height = 30.dp, blurRadius = 40.dp)
            .auraGlow(Color.White.copy(alpha = 0.52f), width = 24.dp, height = 24.dp, blurRadius = 16.dp)
            .auraGlow(Color.White.copy(alpha = 0.40f), width = 26.dp, height = 26.dp, blurRadius = 8.dp)
            .pressScale(interactionSource, pressedScale = 0.9f)
            .drawBehind {
                drawPlanet(
                    bodyColor = colors.surfaceTop,
                    dotColor = colors.mapDotIdle,
                    bodyFraction = PLANET_BODY_FRACTION,
                    haloAlpha = 0f,
                )
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (hasUnread) {
            Box(
                Modifier
                    .size(24.dp)
                    .graphicsLayer()
                    .drawWithCache {
                        val radius = size.minDimension / 2f
                        val brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.danger.copy(alpha = 0.55f),
                            ),
                            radius = radius,
                        )

                        onDrawBehind {
                            drawCircle(
                                brush = brush,
                                radius = radius,
                                alpha = pulse.value,
                            )
                        }
                    }
            )
        }
    }
}
