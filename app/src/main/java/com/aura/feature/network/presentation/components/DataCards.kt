package com.aura.feature.network.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.PRESS_FADE_MILLIS
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.core.network.label
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.IpProtocol

private val CardShape = RoundedCornerShape(16.dp)

private val CardHeight = 62.dp

private val CardGap = 12.dp

@Composable
fun DataCardsGrid(
    connection: ConnectionDetails,
    onVpnCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val empty = stringResource(R.string.net_value_empty)
    val glowRadius = with(LocalDensity.current) { 6.dp.toPx() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
            DataCard(
                label = stringResource(R.string.net_connection),
                value = connection.networkType.label(),
                valueColor = colors.green,
                valueGlow = Shadow(colors.green.copy(alpha = 0.80f), blurRadius = glowRadius),
                modifier = Modifier.weight(1f),
            )
            DataCard(
                label = stringResource(R.string.net_operator),
                value = connection.operator ?: empty,
                valueColor = colors.textBright,
                valueGlow = Shadow(colors.glowIce, blurRadius = glowRadius),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
            DataCard(
                label = stringResource(R.string.net_ip),
                value = connection.ipAddress ?: empty,
                valueColor = colors.textBright,
                valueGlow = Shadow(colors.glowIce, blurRadius = glowRadius),
                modifier = Modifier.weight(1f),
            )
            DataCard(
                label = stringResource(R.string.net_protocol),
                value = connection.protocol?.label() ?: empty,
                valueColor = colors.accentBlue,
                valueGlow = Shadow(colors.glowCyan.copy(alpha = 0.50f), blurRadius = glowRadius),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
            DataCard(
                label = stringResource(R.string.net_location),
                value = connection.location ?: empty,
                valueColor = colors.textBright,
                valueGlow = Shadow(colors.glowIce, blurRadius = glowRadius),
                modifier = Modifier.weight(1f),
            )
            VpnCard(
                isVpnActive = connection.isVpnActive,
                onClick = onVpnCardClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DataCard(
    label: String,
    value: String,
    valueColor: Color,
    valueGlow: Shadow,
    modifier: Modifier = Modifier,
) {
    AuraCard(modifier = modifier.height(CardHeight), shape = CardShape) {
        DataCardContent(
            label = label,
            value = value,
            valueColor = valueColor,
            valueGlow = valueGlow,
        )
    }
}

@Composable
private fun DataCardContent(
    label: String,
    value: String,
    valueColor: Color,
    valueGlow: Shadow,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = AuraTheme.typography.dataValue.copy(shadow = valueGlow),
            color = valueColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun VpnCard(
    isVpnActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)
    val glowRadius = with(LocalDensity.current) { 6.dp.toPx() }

    val alert by animateFloatAsState(
        targetValue = if (isVpnActive) 1f else 0f,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "vpn-alert",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isVpnActive) colors.warning else colors.border,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "vpn-border",
    )

    Box(
        modifier = modifier
            .pressScale(pressed = isPressed, enabled = isVpnActive)
            .height(CardHeight)
            .auraDropShadow(
                color = colors.warning.copy(alpha = alert),
                blurRadius = 4.dp,
                cornerRadius = 16.dp,
            )
            .clip(CardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surfaceTop,
                        lerpToward(colors.surfaceBottom, colors.warning, 0.10f * alert),
                    )
                )
            )
            .border(if (isVpnActive) 1.dp else 0.5.dp, borderColor, CardShape)
            .then(
                if (isVpnActive) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
    ) {
        DataCardContent(
            label = stringResource(R.string.net_vpn),
            value = stringResource(if (isVpnActive) R.string.net_vpn_on else R.string.net_vpn_off),
            valueColor = if (isVpnActive) colors.warning else colors.textSecondary,
            valueGlow = Shadow(
                color = colors.warning.copy(alpha = 0.80f * alert),
                blurRadius = glowRadius,
            ),
        )
    }
}

private fun lerpToward(base: Color, target: Color, fraction: Float): Color = Color(
    red = base.red + (target.red - base.red) * fraction,
    green = base.green + (target.green - base.green) * fraction,
    blue = base.blue + (target.blue - base.blue) * fraction,
    alpha = base.alpha,
)

@Composable
private fun IpProtocol.label(): String = stringResource(
    when (this) {
        IpProtocol.IPV4 -> R.string.net_protocol_ipv4
        IpProtocol.IPV6 -> R.string.net_protocol_ipv6
    }
)
