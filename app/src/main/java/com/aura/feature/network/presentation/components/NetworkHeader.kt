package com.aura.feature.network.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraPill
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.presentation.format.formatBadgeDay
import com.aura.feature.network.presentation.format.formatClockTime
import com.aura.feature.network.presentation.format.isSameDayAs

@Composable
fun NetworkHeader(
    lastTestedAt: Long?,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.net_title),
                style = AuraTheme.typography.screenHeading,
                color = colors.textBright,
            )

            AuraPill(
                text = stringResource(R.string.net_live),
                contentColor = colors.green,
                borderColor = colors.green.copy(alpha = 0.40f),
                backgroundColor = colors.green.copy(alpha = 0.10f),
                leadingDotColor = colors.green,
                borderWidth = 0.5.dp,
                horizontalPadding = 10.dp,
                verticalPadding = 4.dp,
                textStyle = AuraTheme.typography.cardLabel,
                contentShadow = Shadow(color = colors.green.copy(alpha = 0.45f), blurRadius = 6f),
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = lastTestedLabel(lastTestedAt),
            style = AuraTheme.typography.caption,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun lastTestedLabel(lastTestedAt: Long?): String {
    if (lastTestedAt == null) return stringResource(R.string.net_history_empty)

    val time = lastTestedAt.formatClockTime()
    return if (lastTestedAt.isSameDayAs(System.currentTimeMillis())) {
        stringResource(R.string.net_last_tested, time)
    } else {
        stringResource(R.string.net_last_tested_earlier, lastTestedAt.formatBadgeDay(), time)
    }
}
