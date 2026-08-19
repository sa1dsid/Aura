package com.aura.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme

private const val CARD_GLOW_ALPHA = 0.60f

private val CARD_GLOW_BLUR = 8.dp

private val CARD_CORNER = 16.dp

@Composable
fun AuraCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    flat: Boolean = false,
    glow: Boolean = false,
    containerColor: Color? = null,
    borderWidth: Dp = 0.5.dp,
    content: @Composable () -> Unit,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val glowModifier = rememberAuraCardGlow(enabled = glow, color = colors.glowIce)

    val isInteractive = onClick != null && enabled
    val isHighlighted = isPressed && isInteractive

    val topColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> colors.surfaceElevated
            containerColor != null -> containerColor
            else -> colors.surfaceTop
        },
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "card-background-top",
    )
    val bottomColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> colors.surfaceElevated
            containerColor != null -> containerColor
            flat -> colors.surfaceTop
            else -> colors.surfaceBottom
        },
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "card-background-bottom",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) colors.borderStrong else colors.border,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "card-border",
    )

    Box(
        modifier = modifier
            .pressScale(pressed = isPressed, enabled = isInteractive)
            .then(glowModifier)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .border(borderWidth, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

@Composable
private fun rememberAuraCardGlow(enabled: Boolean, color: Color): Modifier =
    remember(enabled, color) {
        if (!enabled) {
            Modifier
        } else {
            Modifier.auraDropShadow(
                color = color.copy(alpha = CARD_GLOW_ALPHA),
                blurRadius = CARD_GLOW_BLUR,
                cornerRadius = CARD_CORNER,
            )
        }
    }

@Composable
fun AuraPill(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = AuraTheme.colors.textPrimary,
    borderColor: Color = AuraTheme.colors.borderStrong,
    backgroundColor: Color = Color.Transparent,
    backgroundBrush: Brush? = null,
    leadingDotColor: Color? = null,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 5.dp,
    topPadding: Dp = verticalPadding,
    bottomPadding: Dp = verticalPadding,
    borderWidth: Dp = 1.dp,
    contentShadow: Shadow? = null,
    textStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)
    val isHighlighted = isPressed && onClick != null

    val background by animateColorAsState(
        targetValue = if (isHighlighted) borderColor.copy(alpha = 0.22f) else backgroundColor,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "pill-background",
    )

    Row(
        modifier = modifier
            .pressScale(pressed = isPressed, enabled = onClick != null, pressedScale = 0.94f)
            .clip(CircleShape)
            .then(if (backgroundBrush != null) Modifier.background(backgroundBrush) else Modifier)
            .background(background)
            .border(borderWidth, borderColor, CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .padding(
                start = horizontalPadding,
                top = topPadding,
                end = horizontalPadding,
                bottom = bottomPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingDotColor != null) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(leadingDotColor)
            )
        }
        Text(
            text = text,
            style = (textStyle ?: AuraTheme.typography.badge).copy(shadow = contentShadow),
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}
