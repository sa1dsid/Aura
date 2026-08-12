package com.aura.feature.network.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.format.StyledArg
import com.aura.feature.home.presentation.format.annotatedFormat
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.presentation.format.NetworkLogLine
import com.aura.feature.network.presentation.format.formatClockTime
import com.aura.feature.network.presentation.format.formatLogDay
import com.aura.feature.network.presentation.format.toLogLines

private val LogMaxHeight = 210.dp

@Composable
fun NetworkLogCard(
    history: List<PingRecord>,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val lines = history.toLogLines()

    AuraCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.net_log_title, history.size),
                style = AuraTheme.typography.caption,
                color = colors.textSecondary,
                maxLines = 1,
            )

            Spacer(Modifier.height(10.dp))

            if (lines.isEmpty()) {
                Text(
                    text = stringResource(R.string.net_history_empty),
                    style = AuraTheme.typography.consoleLine,
                    color = colors.textTertiary,
                )
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = LogMaxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                lines.forEach { line ->
                    when (line) {
                        is NetworkLogLine.DayComment -> Text(
                            text = stringResource(
                                R.string.net_log_day,
                                line.timestamp.formatLogDay(),
                                line.count,
                            ),
                            style = AuraTheme.typography.consoleLine,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        )

                        is NetworkLogLine.Entry -> Text(
                            text = line.record.toLogText(),
                            style = AuraTheme.typography.consoleLine,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PingRecord.toLogText(): AnnotatedString {
    val colors = AuraTheme.colors
    val empty = stringResource(R.string.net_value_empty)

    return annotatedFormat(
        stringResource(R.string.net_log_line),
        StyledArg(timestamp.formatClockTime(), SpanStyle(color = colors.textBright)),
        StyledArg(ipAddress ?: empty),
        StyledArg(operator ?: empty),
        StyledArg(
            stringResource(R.string.net_ping_value, pingMs),
            SpanStyle(color = if (vpnActive) colors.danger else colors.accentBlue),
        ),
        StyledArg(location ?: empty),
        StyledArg(
            stringResource(if (vpnActive) R.string.net_log_vpn_on else R.string.net_log_vpn_off),
            SpanStyle(color = if (vpnActive) colors.danger else colors.textTertiary),
        ),
    )
}
