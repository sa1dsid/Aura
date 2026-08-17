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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme

@Composable
fun AuraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 53.dp,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val background by animateColorAsState(
        targetValue = when {
            !enabled -> colors.authButtonDisabled
            isPressed -> colors.authSkipLabel
            else -> colors.authPrimaryButton
        },
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "primary-button-background",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(pressed = isPressed, enabled = enabled)
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.primaryButtonLabel,
            color = if (enabled) Color.Black else colors.authTextDim,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AuraOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    borderColor: Color = AuraTheme.colors.authOutline,
    contentColor: Color = AuraTheme.colors.authSkipLabel,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val background by animateColorAsState(
        targetValue = if (isPressed) colors.authSurface else Color.Black,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "outlined-button-background",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(pressed = isPressed, enabled = enabled)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.secondaryButtonLabel,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AuraSurfaceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    leading: @Composable (() -> Unit)? = null,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val background by animateColorAsState(
        targetValue = if (isPressed) colors.authSegmentActive else colors.authSurface,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "surface-button-background",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(pressed = isPressed, enabled = enabled)
            .clip(shape)
            .background(background)
            .border(1.dp, colors.authBorderSoft, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(
            text = text,
            style = AuraTheme.typography.actionLabel,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}
