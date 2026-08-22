package com.aura.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.theme.AuraTheme

private const val PLANET_BODY_FRACTION = 21.5f / 48f

private const val PULSE_MILLIS = 700

private const val PULSE_MIN_ALPHA = 0.45f

private const val ALERT_FADE_MILLIS = 400

private val PlanetSize = 48.dp

private val FarGlowSize = 30.dp

private val MediumGlowSize = 24.dp

private val NearGlowSize = 26.dp

private const val FAR_GLOW_ALPHA = 0.40f

private const val MEDIUM_GLOW_ALPHA = 0.52f

private const val NEAR_GLOW_ALPHA = 0.40f

private val FarGlowBlur = 40.dp

private val MediumGlowBlur = 16.dp

private val NearGlowBlur = 8.dp

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

    val alert = animateFloatAsState(
        targetValue = if (hasUnread) 1f else 0f,
        animationSpec = tween(ALERT_FADE_MILLIS),
        label = "news-alert",
    )

    val pulse = rememberInfiniteTransition(label = "news-planet").animateFloat(
        initialValue = PULSE_MIN_ALPHA,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "news-pulse",
    )

    val calmAlpha = { 1f - alert.value }
    val alertAlpha = { alert.value * pulse.value }

    Box(
        modifier = modifier
            .size(PlanetSize)
            .planetGlow(Color.White, calmAlpha)
            .planetGlow(colors.warning, alertAlpha)
            .pressScale(interactionSource, pressedScale = 0.9f)
            .drawBehind {
                drawPlanet(
                    bodyColor = colors.surfaceTop,
                    dotColor = colors.newsPlanetDot,
                    bodyFraction = PLANET_BODY_FRACTION,
                    haloAlpha = 0f,
                )
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = stringResource(R.string.cd_news),
                onClick = onClick,
            ),
    )
}

private fun Modifier.planetGlow(color: Color, alpha: () -> Float): Modifier = this
    .auraGlow(
        color = color.copy(alpha = FAR_GLOW_ALPHA),
        width = FarGlowSize,
        height = FarGlowSize,
        blurRadius = FarGlowBlur,
        alpha = alpha,
    )
    .auraGlow(
        color = color.copy(alpha = MEDIUM_GLOW_ALPHA),
        width = MediumGlowSize,
        height = MediumGlowSize,
        blurRadius = MediumGlowBlur,
        alpha = alpha,
    )
    .auraGlow(
        color = color.copy(alpha = NEAR_GLOW_ALPHA),
        width = NearGlowSize,
        height = NearGlowSize,
        blurRadius = NearGlowBlur,
        alpha = alpha,
    )
