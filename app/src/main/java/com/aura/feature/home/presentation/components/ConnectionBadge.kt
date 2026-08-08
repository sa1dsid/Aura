package com.aura.feature.home.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraPill
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.ConnectionState

@Composable
fun ConnectionBadge(
    connection: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    val text = buildString {
        append(connection.networkType.label)
        append("·")
        if (connection.isVpnActive) {
            append("VPN ON·paused")
        } else {
            append("VPN OFF·+${connection.rewardIon} ION")
        }
    }

    AuraPill(
        text = text,
        modifier = modifier,
        contentColor = if (connection.isVpnActive) colors.danger else colors.textPrimary,
        borderColor = if (connection.isVpnActive) colors.danger.copy(alpha = 0.5f) else colors.border,
        backgroundColor = colors.surface,
        leadingDotColor = if (connection.isVpnActive) colors.danger else colors.green,
        horizontalPadding = 14.dp,
        verticalPadding = 8.dp,
        onClick = if (connection.isClickable) onClick else null,
    )
}
