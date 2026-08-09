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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.PRESS_FADE_MILLIS
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.ConnectionState

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

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.textBright)) {
                    append(connection.networkType.label)
                }
                withStyle(SpanStyle(color = colors.textSecondary)) { append(" · ") }
                if (connection.isVpnActive) {
                    withStyle(SpanStyle(color = colors.textBright)) { append("VPN ON") }
                    withStyle(SpanStyle(color = colors.textSecondary)) { append(" · ") }
                    withStyle(SpanStyle(color = colors.danger)) { append("paused") }
                } else {
                    withStyle(SpanStyle(color = colors.textBright)) { append("VPN OFF") }
                    withStyle(SpanStyle(color = colors.textSecondary)) { append(" · ") }
                    withStyle(SpanStyle(color = colors.accentBlue)) {
                        append("+${connection.rewardIon} ION")
                    }
                }
            },
            style = AuraTheme.typography.caption,
        )
    }
}
