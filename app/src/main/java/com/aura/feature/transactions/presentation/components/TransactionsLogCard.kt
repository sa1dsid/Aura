package com.aura.feature.transactions.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.common.LogValue
import com.aura.core.common.formatClock
import com.aura.core.common.formatDayShort
import com.aura.core.common.logLine
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraLogScrollBar
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.component.brightDotShadows
import com.aura.core.designsystem.component.scrollProgress
import com.aura.core.designsystem.component.visibleFraction
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.presentation.format.TransactionLogLine
import com.aura.feature.transactions.presentation.format.toLogLines

@Composable
fun TransactionsLogCard(
    events: List<TransactionEvent>,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val lines = events.toLogLines()
    val scrollState = rememberScrollState()

    AuraCard(
        modifier = modifier
            .fillMaxWidth()
            .auraDropShadow(
                color = colors.glowIce.copy(alpha = 0.60f),
                blurRadius = 8.dp,
                cornerRadius = 16.dp,
                spread = (-6).dp,
            ),
    ) {
        Column(Modifier.fillMaxSize()) {
            LogHeader(count = events.size)

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clipToBounds()
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    lines.forEachIndexed { index, line ->
                        LogRow(number = index + 1, line = line)
                    }

                    if (scrollState.maxValue > 0) {
                        CommentRow(
                            number = lines.size + 1,
                            text = stringResource(R.string.tx_log_more),
                        )
                    }
                }

                AuraLogScrollBar(
                    fraction = scrollState.scrollProgress(),
                    visibleFraction = scrollState.visibleFraction(),
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
            text = stringResource(R.string.tx_log_title, count),
            style = AuraTheme.typography.caption,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LogRow(number: Int, line: TransactionLogLine, modifier: Modifier = Modifier) {
    when (line) {
        is TransactionLogLine.DayComment -> CommentRow(
            number = number,
            text = stringResource(
                R.string.tx_log_day,
                line.timestamp.formatDayShort(),
                line.count,
            ),
            modifier = modifier,
        )

        is TransactionLogLine.Entry -> NumberedRow(number = number, modifier = modifier) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = line.event.headLine(),
                    style = AuraTheme.typography.caption,
                )
                Text(
                    text = line.event.tailLine(),
                    style = AuraTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun CommentRow(number: Int, text: String, modifier: Modifier = Modifier) {
    NumberedRow(number = number, modifier = modifier) {
        Text(
            text = text,
            style = AuraTheme.typography.caption.copy(fontStyle = FontStyle.Italic),
            color = AuraTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun NumberedRow(
    number: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.net_log_number, number),
            style = AuraTheme.typography.logNumber,
            color = AuraTheme.colors.textDisabled,
        )
        content()
    }
}

@Composable
private fun TransactionEvent.headLine(): AnnotatedString {
    val colors = AuraTheme.colors

    return logLine(
        template = stringResource(R.string.tx_log_line_head),
        keyStyle = SpanStyle(color = colors.accentBlue),
        punctuationStyle = SpanStyle(color = colors.textDisabled),
        values = listOf(
            LogValue(timestamp.formatClock(), SpanStyle(color = colors.accentBlue)),
            LogValue(typeLabel, SpanStyle(color = colors.textBright)),
            LogValue(fieldKey, SpanStyle(color = colors.accentBlue)),
            LogValue(fieldValue, SpanStyle(color = colors.textBright)),
        ),
    )
}

@Composable
private fun TransactionEvent.tailLine(): AnnotatedString {
    val colors = AuraTheme.colors

    return logLine(
        template = stringResource(R.string.tx_log_line_tail),
        keyStyle = SpanStyle(color = colors.accentBlue),
        punctuationStyle = SpanStyle(color = colors.textDisabled),
        values = listOf(
            LogValue(
                text = amount,
                style = SpanStyle(color = if (isCredit) colors.green else colors.textSecondary),
            ),
        ),
    )
}
