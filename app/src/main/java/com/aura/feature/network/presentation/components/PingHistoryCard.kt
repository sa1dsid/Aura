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

private val LegendLineWidth = 10.dp

private val LegendLineHeight = 2.dp

private val PlotInset = 11.5.dp

@Composable
fun PingHistoryCard(
    history: List<PingRecord>,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.net_ping_history),
                    style = AuraTheme.typography.cardTitle,
                    color = colors.textBright,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.net_ping_history_sub, PING_HISTORY_LIMIT),
                    style = AuraTheme.typography.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (history.isEmpty()) {
                EmptyChart(Modifier.padding(horizontal = 16.dp))
            } else {
                PingChart(
                    history = history,
                    modifier = Modifier.padding(horizontal = PlotInset),
                )
            }

            Spacer(Modifier.height(16.dp))

            Legend(Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun EmptyChart(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(239.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.net_history_empty),
            style = AuraTheme.typography.caption,
            color = colors.textDisabled,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(stringResource(R.string.net_legend_ideal), colors.green)
            LegendItem(stringResource(R.string.net_legend_good), colors.accentBlue)
            LegendItem(stringResource(R.string.net_legend_warning), colors.warning)
        }

        LegendItem(
            text = stringResource(R.string.net_legend_vpn),
            color = colors.warning,
            isRing = true,
        )
    }
}

@Composable
private fun LegendItem(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    isRing: Boolean = false,
) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isRing) {
            Box(
                Modifier
                    .size(8.dp)
                    .border(1.dp, color, CircleShape)
            )
        } else {
            Box(
                Modifier
                    .size(width = LegendLineWidth, height = LegendLineHeight)
                    .background(color)
            )
        }
        Text(
            text = text,
            style = AuraTheme.typography.caption,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}
