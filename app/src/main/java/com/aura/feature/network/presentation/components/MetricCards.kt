package com.aura.feature.network.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.presentation.format.formatPacketLoss

@Composable
fun MetricCardsRow(
    metrics: NetworkMetrics,
    modifier: Modifier = Modifier,
) {
    val empty = stringResource(R.string.net_value_empty)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MetricCard(
            label = stringResource(R.string.net_ping),
            value = metrics.pingMs?.toString() ?: empty,
            unit = stringResource(R.string.unit_ms),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.net_jitter),
            value = metrics.jitterMs?.toString() ?: empty,
            unit = stringResource(R.string.unit_ms),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.net_packet_loss),
            value = metrics.packetLossPercent?.formatPacketLoss() ?: empty,
            unit = stringResource(R.string.unit_percent),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = AuraTheme.typography.metricNumber,
                color = colors.textBright,
                maxLines = 1,
            )
            Text(
                text = unit,
                style = AuraTheme.typography.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
