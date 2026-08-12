package com.aura.feature.network.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.model.PING_WARNING_MS
import com.aura.feature.network.presentation.format.formatPacketLoss

@Composable
fun MetricCardsRow(
    metrics: NetworkMetrics,
    modifier: Modifier = Modifier,
) {
    val empty = stringResource(R.string.net_value_empty)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricCard(
            label = stringResource(R.string.net_ping),
            value = metrics.pingMs?.toString() ?: empty,
            unit = stringResource(R.string.unit_ms),
            isAlarming = (metrics.pingMs ?: 0) > PING_WARNING_MS,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.net_jitter),
            value = metrics.jitterMs?.toString() ?: empty,
            unit = stringResource(R.string.unit_ms),
            isAlarming = false,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.net_packet_loss),
            value = metrics.packetLossPercent?.formatPacketLoss() ?: empty,
            unit = stringResource(R.string.unit_percent),
            isAlarming = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    isAlarming: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 13.dp)) {
            Text(
                text = label,
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = AuraTheme.typography.displayNumber,
                    color = if (isAlarming) colors.danger else colors.textBright,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = AuraTheme.typography.unitLabel,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}
