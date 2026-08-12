package com.aura.feature.network.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraPrimaryButton
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.domain.model.ConnectionGrade
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.model.SpeedTestState
import com.aura.feature.network.presentation.format.formatScore
import com.aura.feature.network.presentation.format.formatSpeed

private val GaugeDiameter = 168.dp

private val SkeletonShape = RoundedCornerShape(4.dp)

@Composable
fun DiagnosticsCard(
    diagnostics: SpeedTestState,
    onStartTestClick: () -> Unit,
    onShareResultClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (diagnostics) {
                SpeedTestState.Idle -> IdleGauge(onStartTestClick = onStartTestClick)
                is SpeedTestState.Running -> RunningGauge(progress = diagnostics.progress)
                is SpeedTestState.Done -> ResultGauge(result = diagnostics.result)
            }

            Spacer(Modifier.height(18.dp))

            when (diagnostics) {
                SpeedTestState.Idle -> AuraPrimaryButton(
                    text = stringResource(R.string.diag_start),
                    onClick = onStartTestClick,
                    height = 46.dp,
                    shape = RoundedCornerShape(14.dp),
                )

                is SpeedTestState.Running -> SpeedRow(
                    downloadMbps = null,
                    uploadMbps = null,
                    pingMs = null,
                    valueColor = colors.textBright,
                )

                is SpeedTestState.Done -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SpeedRow(
                        downloadMbps = diagnostics.result.downloadMbps,
                        uploadMbps = diagnostics.result.uploadMbps,
                        pingMs = diagnostics.result.pingMs,
                        valueColor = diagnostics.result.grade.valueColor(),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AuraPrimaryButton(
                            text = stringResource(R.string.diag_test_again),
                            onClick = onStartTestClick,
                            height = 46.dp,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        )
                        ShareButton(onClick = onShareResultClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleGauge(onStartTestClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    SpeedGauge(
        progress = 0f,
        accentColor = colors.accentBlue,
        diameter = GaugeDiameter,
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.97f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onStartTestClick,
            ),
    ) {
        Text(
            text = stringResource(R.string.diag_tap_to_test),
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RunningGauge(progress: Float, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    SpeedGauge(
        progress = progress,
        accentColor = colors.accentBlue,
        diameter = GaugeDiameter,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.diag_testing),
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultGauge(result: SpeedTestResult, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val accent = result.grade.valueColor()

    SpeedGauge(
        progress = result.grade.filledDots / ConnectionGrade.EXCELLENT.filledDots.toFloat(),
        accentColor = accent,
        diameter = GaugeDiameter,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = result.grade.formatScore(),
                style = AuraTheme.typography.displayNumber,
                color = accent,
            )
            Spacer(Modifier.height(6.dp))
            GradeDots(grade = result.grade, color = accent)
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(result.grade.labelRes()),
                style = AuraTheme.typography.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GradeDots(grade: ConnectionGrade, color: Color, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val total = ConnectionGrade.EXCELLENT.filledDots

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (index < grade.filledDots) color else colors.border)
            )
        }
    }
}

@Composable
private fun SpeedRow(
    downloadMbps: Double?,
    uploadMbps: Double?,
    pingMs: Int?,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SpeedValue(
            label = stringResource(R.string.diag_download),
            value = downloadMbps?.formatSpeed(),
            unit = stringResource(R.string.unit_mbps),
            valueColor = valueColor,
        )
        SpeedValue(
            label = stringResource(R.string.diag_upload),
            value = uploadMbps?.formatSpeed(),
            unit = stringResource(R.string.unit_mbps),
            valueColor = valueColor,
        )
        SpeedValue(
            label = stringResource(R.string.net_ping),
            value = pingMs?.toString(),
            unit = stringResource(R.string.unit_ms),
            valueColor = valueColor,
        )
    }
}

@Composable
private fun SpeedValue(
    label: String,
    value: String?,
    unit: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(6.dp))
        if (value == null) {
            Box(
                Modifier
                    .width(44.dp)
                    .height(20.dp)
                    .clip(SkeletonShape)
                    .background(colors.border)
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = AuraTheme.typography.counterNumber,
                    color = valueColor,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = AuraTheme.typography.unitLabel,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ShareButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.92f)
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_send_2),
            contentDescription = stringResource(R.string.cd_share),
            tint = colors.textBright,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ConnectionGrade.valueColor(): Color = when (this) {
    ConnectionGrade.EXCELLENT -> AuraTheme.colors.mint
    ConnectionGrade.GOOD -> AuraTheme.colors.accentBlue
    ConnectionGrade.POOR -> AuraTheme.colors.danger
}

private fun ConnectionGrade.labelRes(): Int = when (this) {
    ConnectionGrade.EXCELLENT -> R.string.diag_conn_excellent
    ConnectionGrade.GOOD -> R.string.diag_conn_good
    ConnectionGrade.POOR -> R.string.diag_conn_poor
}
