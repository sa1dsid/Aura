package com.aura.feature.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.PRESS_FADE_MILLIS
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.ConnectionState
import com.aura.feature.home.presentation.format.label

private val BadgeShape = RoundedCornerShape(16.dp)

@Composable
fun ConnectionBadge(
    connection: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)
    val isClickable = connection.isClickable

    val background by animateColorAsState(
        targetValue = colors.glowSky.copy(alpha = if (isPressed && isClickable) 0.12f else 0.04f),
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "connection-badge-background",
    )

    val dotColor = if (connection.isVpnActive) colors.danger else colors.green

    Row(
        modifier = modifier
            .pressScale(pressed = isPressed, enabled = isClickable, pressedScale = 0.96f)
            .height(29.dp)
            .clip(BadgeShape)
            .background(background)
            .border(0.5.dp, colors.border, BadgeShape)
            .then(
                if (isClickable) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .auraGlowLayers(listOf(AuraShadow(dotColor.copy(alpha = 0.80f), 6.dp)))
                .clip(CircleShape)
                .background(dotColor)
        )

        val text = stringResource(
            if (connection.isVpnActive) R.string.badge_conn_paused else R.string.badge_conn_ok,
            connection.networkType.label(),
            connection.rewardIon,
        )
        val stateColor = if (connection.isVpnActive) colors.danger else colors.accentBlue

        Text(
            text = buildAnnotatedString {
                val segments = text.split('·')
                segments.forEachIndexed { index, segment ->
                    if (index > 0) {
                        withStyle(SpanStyle(color = colors.textSecondary)) { append(" · ") }
                    }
                    val color =
                        if (index == segments.lastIndex && index > 0) stateColor
                        else colors.textBright
                    withStyle(SpanStyle(color = color)) { append(segment) }
                }
            },
            style = AuraTheme.typography.caption,
        )
    }
}
