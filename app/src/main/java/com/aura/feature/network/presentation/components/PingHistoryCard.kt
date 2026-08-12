package com.aura.feature.network.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.domain.model.PING_HISTORY_LIMIT
import com.aura.feature.network.domain.model.PingRecord

private val ChartHeight = 132.dp

@Composable
fun PingHistoryCard(
    history: List<PingRecord>,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.net_ping_history),
                        style = AuraTheme.typography.title,
                        color = colors.textBright,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.net_ping_history_sub, PING_HISTORY_LIMIT),
                        style = AuraTheme.typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }

                history.lastOrNull()?.let { latest -> LatestBadge(record = latest) }
            }

            Spacer(Modifier.height(14.dp))

            if (history.isEmpty()) {
                EmptyChart()
            } else {
                PingChart(
                    history = history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ChartHeight),
                )

                Spacer(Modifier.height(8.dp))

                AxisLabels()
            }

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            Spacer(Modifier.height(12.dp))

            Legend()
        }
    }
}

@Composable
private fun LatestBadge(record: PingRecord, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.accentBlue.copy(alpha = 0.10f))
            .border(0.5.dp, colors.accentBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = stringResource(R.string.net_latest),
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )
        Text(
            text = stringResource(R.string.net_ping_value, record.pingMs),
            style = AuraTheme.typography.counterNumber,
            color = if (record.vpnActive) colors.danger else colors.textBright,
        )
    }
}

@Composable
private fun EmptyChart(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.net_history_empty),
            style = AuraTheme.typography.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AxisLabels(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.net_axis_first),
            style = AuraTheme.typography.stepLabel,
            color = colors.textTertiary,
        )
        Text(
            text = stringResource(R.string.net_axis_mid),
            style = AuraTheme.typography.stepLabel,
            color = colors.textTertiary,
        )
        Text(
            text = stringResource(R.string.net_axis_last),
            style = AuraTheme.typography.stepLabel,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(stringResource(R.string.net_legend_ideal), colors.mint, filled = true)
        LegendItem(stringResource(R.string.net_legend_good), colors.accentBlue, filled = true)
        LegendItem(stringResource(R.string.net_legend_warning), colors.danger, filled = true)
        LegendItem(stringResource(R.string.net_legend_vpn), colors.danger, filled = false)
    }
}

@Composable
private fun LegendItem(
    text: String,
    color: Color,
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .then(
                    if (filled) Modifier.background(color)
                    else Modifier.border(1.dp, color, CircleShape)
                )
        )
        Text(
            text = text,
            style = AuraTheme.typography.stepLabel,
            color = colors.textTertiary,
            maxLines = 1,
        )
    }
}
