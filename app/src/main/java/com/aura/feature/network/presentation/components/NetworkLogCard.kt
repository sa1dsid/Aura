package com.aura.feature.network.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.common.LogValue
import com.aura.core.common.logLine
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraLogScrollBar
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.component.brightDotShadows
import com.aura.core.designsystem.component.scrollProgress
import com.aura.core.designsystem.component.visibleFraction
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.presentation.format.NetworkLogLine
import com.aura.feature.network.presentation.format.formatClockTime
import com.aura.feature.network.presentation.format.formatLogDay
import com.aura.feature.network.presentation.format.toLogLines

private val LogHeight = 253.dp

private val KeepScrollInside = object : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = available

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

@Composable
fun NetworkLogCard(
    history: List<PingRecord>,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val lines = remember(history) { history.toLogLines() }
    val scrollState = rememberScrollState()

    AuraCard(modifier = modifier.fillMaxWidth(), glow = true) {
        Column {
            LogHeader(count = history.size)

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            Row(Modifier.height(LogHeight)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .nestedScroll(KeepScrollInside)
                        .verticalScroll(scrollState)
                        .padding(
                            start = 16.dp,
                            end = 12.dp,
                            top = 12.dp,
                            bottom = 12.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (lines.isEmpty()) {
                        Text(
                            text = stringResource(R.string.net_history_empty),
                            style = AuraTheme.typography.caption,
                            color = colors.textDisabled,
                        )
                    }

                    lines.forEachIndexed { index, line ->
                        LogRow(number = index + 1, line = line)
                    }
                }

                AuraLogScrollBar(
                    fraction = scrollState::scrollProgress,
                    visibleFraction = scrollState::visibleFraction,
                )
            }
        }
    }
}

@Composable
private fun LogHeader(count: Int, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .auraGlowLayers(colors.brightDotShadows)
                .clip(CircleShape)
                .background(colors.textBright)
        )
        Text(
            text = stringResource(R.string.net_log_title, count),
            style = AuraTheme.typography.caption,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LogRow(number: Int, line: NetworkLogLine, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.net_log_number, number),
            style = AuraTheme.typography.logNumber,
            color = colors.textDisabled,
        )

        when (line) {
            is NetworkLogLine.DayComment -> Text(
                text = stringResource(
                    R.string.net_log_day,
                    line.timestamp.formatLogDay(),
                    line.count,
                ),
                style = AuraTheme.typography.caption.copy(fontStyle = FontStyle.Italic),
                color = colors.textSecondary,
            )

            is NetworkLogLine.Entry -> Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = line.record.headLine(),
                    style = AuraTheme.typography.caption,
                    maxLines = 1,
                )
                Text(
                    text = line.record.tailLine(),
                    style = AuraTheme.typography.caption,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PingRecord.headLine(): AnnotatedString {
    val colors = AuraTheme.colors
    val empty = stringResource(R.string.net_value_empty)

    return logLine(
        template = stringResource(R.string.net_log_line_head),
        keyStyle = SpanStyle(color = colors.accentBlue),
        punctuationStyle = SpanStyle(color = colors.textDisabled),
        values = listOf(
            LogValue(timestamp.formatClockTime(), SpanStyle(color = colors.accentBlue)),
            LogValue(ipAddress ?: empty, SpanStyle(color = colors.textBright)),
            LogValue(operator ?: empty, SpanStyle(color = colors.textBright)),
            LogValue(
                text = stringResource(R.string.net_ping_value, pingMs),
                style = SpanStyle(color = if (vpnActive) colors.warning else colors.green),
            ),
        ),
    )
}

@Composable
private fun PingRecord.tailLine(): AnnotatedString {
    val colors = AuraTheme.colors
    val empty = stringResource(R.string.net_value_empty)
    val vpnLabel = stringResource(
        if (vpnActive) R.string.net_log_vpn_on else R.string.net_log_vpn_off
    )

    return logLine(
        template = stringResource(R.string.net_log_line_tail),
        keyStyle = SpanStyle(color = colors.accentBlue),
        punctuationStyle = SpanStyle(color = colors.textDisabled),
        values = listOf(
            LogValue(location ?: empty, SpanStyle(color = colors.textBright)),
            LogValue(
                text = vpnLabel,
                style = SpanStyle(color = if (vpnActive) colors.warning else colors.textBright),
            ),
        ),
    )
}

