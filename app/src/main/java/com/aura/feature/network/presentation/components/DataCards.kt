package com.aura.feature.network.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.PRESS_FADE_MILLIS
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.core.network.label
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.IpProtocol

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun DataCardsGrid(
    connection: ConnectionDetails,
    onVpnCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val empty = stringResource(R.string.net_value_empty)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataCard(
                label = stringResource(R.string.net_connection),
                value = connection.networkType.label(),
                valueColor = colors.mint,
                modifier = Modifier.weight(1f),
            )
            DataCard(
                label = stringResource(R.string.net_operator),
                value = connection.operator ?: empty,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataCard(
                label = stringResource(R.string.net_ip),
                value = connection.ipAddress ?: empty,
                modifier = Modifier.weight(1f),
            )
            DataCard(
                label = stringResource(R.string.net_protocol),
                value = connection.protocol?.label() ?: empty,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataCard(
                label = stringResource(R.string.net_location),
                value = connection.location ?: empty,
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
    modifier: Modifier = Modifier,
    valueColor: Color = AuraTheme.colors.textBright,
    onClick: (() -> Unit)? = null,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier, shape = CardShape, onClick = onClick) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = AuraTheme.typography.body,
                color = valueColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun VpnCard(
    isVpnActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    val valueColor by animateColorAsState(
        targetValue = if (isVpnActive) colors.danger else colors.textBright,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "vpn-value",
    )
    val alertBorder by animateColorAsState(
        targetValue = if (isVpnActive) colors.danger.copy(alpha = 0.70f) else Color.Transparent,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "vpn-border",
    )

    DataCard(
        label = stringResource(R.string.net_vpn),
        value = stringResource(if (isVpnActive) R.string.net_vpn_on else R.string.net_vpn_off),
        valueColor = valueColor,
        onClick = if (isVpnActive) onClick else null,
        modifier = modifier.border(1.dp, alertBorder, CardShape),
    )
}

@Composable
private fun IpProtocol.label(): String = stringResource(
    when (this) {
        IpProtocol.IPV4 -> R.string.net_protocol_ipv4
        IpProtocol.IPV6 -> R.string.net_protocol_ipv6
    }
)
