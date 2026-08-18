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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.activeDotShadows
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.network.domain.model.ConnectionGrade
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.model.SpeedTestState
import com.aura.feature.network.presentation.format.formatScore
import com.aura.feature.network.presentation.format.formatSpeed

private val SkeletonShape = RoundedCornerShape(2.dp)

private val ScoreDotSize = 6.dp

@Composable
fun DiagnosticsCard(
    diagnostics: SpeedTestState,
    onStartTestClick: () -> Unit,
    onShareResultClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val result = (diagnostics as? SpeedTestState.Done)?.result

    AuraCard(modifier = modifier.fillMaxWidth(), glow = true) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.diag_scale_mark),
                style = AuraTheme.typography.gaugeLabel,
                color = colors.textBright,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(2.dp))

            val gaugeInteraction = remember { MutableInteractionSource() }

            SpeedGauge(
                litFraction = diagnostics.litFraction(),
                modifier = Modifier.clickable(
                    interactionSource = gaugeInteraction,
                    indication = null,
                    enabled = diagnostics !is SpeedTestState.Running,
                    onClick = onStartTestClick,
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    when (diagnostics) {
                        SpeedTestState.Idle -> Text(
                            text = stringResource(R.string.diag_tap_to_test),
                            style = AuraTheme.typography.dataValue,
                            color = colors.textSecondary,
                        )

                        is SpeedTestState.Running -> RunningReadout()
                        is SpeedTestState.Done -> ResultReadout(result = diagnostics.result)
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            ScaleBounds()

            if (result != null) {
                Spacer(Modifier.height(18.dp))
                ScoreRow(grade = result.grade)
            }

            Spacer(Modifier.height(27.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GlowButton(
                    text = stringResource(
                        if (result == null) R.string.diag_start else R.string.diag_test_again
                    ),
                    enabled = diagnostics !is SpeedTestState.Running,
                    onClick = onStartTestClick,
                )

                if (result != null) {
                    ShareButton(onClick = onShareResultClick)
                }
            }
        }
    }
}

private fun SpeedTestState.litFraction(): Float = when (this) {
    SpeedTestState.Idle -> 0f
    is SpeedTestState.Running -> progress
    is SpeedTestState.Done -> 1f
}

@Composable
private fun ScaleBounds(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.diag_scale_min),
            style = AuraTheme.typography.gaugeLabel,
            color = colors.textSecondary,
        )
        Text(
            text = stringResource(R.string.diag_scale_max),
            style = AuraTheme.typography.gaugeLabel,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun RunningReadout(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        SkeletonReadout(
            label = stringResource(R.string.diag_download),
            arrow = stringResource(R.string.diag_arrow_down),
            unit = stringResource(R.string.unit_mbps),
        )
        SkeletonReadout(
            label = stringResource(R.string.diag_upload),
            arrow = stringResource(R.string.diag_arrow_up),
            unit = stringResource(R.string.unit_mbps),
        )
        Text(
            text = stringResource(R.string.diag_testing),
            style = AuraTheme.typography.dataValue,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun SkeletonReadout(
    label: String,
    arrow: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ReadoutLabel(label = label, arrow = arrow)
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(2) {
                Box(
                    Modifier
                        .size(width = 14.dp, height = 4.dp)
                        .clip(SkeletonShape)
                        .background(colors.textBright)
                )
            }
            Text(
                text = unit,
                style = AuraTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

@Composable
private fun ResultReadout(result: SpeedTestResult, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        ValueReadout(
            label = stringResource(R.string.diag_download),
            arrow = stringResource(R.string.diag_arrow_down),
            value = result.downloadMbps.formatSpeed(),
            unit = stringResource(R.string.unit_mbps),
            grade = result.grade,
        )
        ValueReadout(
            label = stringResource(R.string.diag_upload),
            arrow = stringResource(R.string.diag_arrow_up),
            value = result.uploadMbps.formatSpeed(),
            unit = stringResource(R.string.unit_mbps),
            grade = result.grade,
        )
        ValueReadout(
            label = stringResource(R.string.diag_ping),
            arrow = null,
            value = result.pingMs.toString(),
            unit = stringResource(R.string.unit_ms),
            grade = result.grade,
        )
    }
}

@Composable
private fun ValueReadout(
    label: String,
    arrow: String?,
    value: String,
    unit: String,
    grade: ConnectionGrade,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val glowRadius = with(LocalDensity.current) { 6.dp.toPx() }
    val isPoor = grade == ConnectionGrade.POOR

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ReadoutLabel(label = label, arrow = arrow)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = AuraTheme.typography.speedValue.copy(
                    shadow = Shadow(
                        color = if (isPoor) colors.warning else colors.glowIce,
                        blurRadius = glowRadius,
                    )
                ),
                color = if (isPoor) colors.warning else colors.textBright,
                maxLines = 1,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = unit,
                style = AuraTheme.typography.caption,
                color = colors.textSecondary,
                softWrap = false,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
    }
}

@Composable
private fun ReadoutLabel(label: String, arrow: String?, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Text(
        text = buildAnnotatedString {
            append(label)
            if (arrow != null) {
                append(" ")
                withStyle(SpanStyle(color = colors.green)) { append(arrow) }
            }
        },
        style = AuraTheme.typography.caption,
        color = colors.textSecondary,
        modifier = modifier,
    )
}

@Composable
private fun ScoreRow(grade: ConnectionGrade, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val glowRadius = with(LocalDensity.current) { 6.dp.toPx() }
    val isPoor = grade == ConnectionGrade.POOR
    val accent = if (isPoor) colors.warning else colors.green

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = grade.formatScore(),
            style = AuraTheme.typography.speedValue.copy(
                shadow = Shadow(
                    color = if (isPoor) colors.warning else colors.glowIce,
                    blurRadius = glowRadius,
                )
            ),
            color = if (isPoor) colors.warning else colors.textBright,
        )

        Column(
            modifier = Modifier.padding(top = 10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(ConnectionGrade.EXCELLENT.filledDots) { index ->
                    ScoreDot(isFilled = index < grade.filledDots, color = accent)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = stringResource(grade.labelRes()),
                style = AuraTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
    }
}

@Composable
private fun ScoreDot(isFilled: Boolean, color: Color, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Box(
        modifier = modifier
            .size(ScoreDotSize)
            .then(if (isFilled) Modifier.auraGlowLayers(colors.activeDotShadows) else Modifier)
            .clip(CircleShape)
            .background(if (isFilled) color else colors.textDisabled)
    )
}

@Composable
private fun GlowButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(50.dp)
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.96f)
            .auraDropShadow(
                color = Color.White.copy(alpha = 0.30f * alpha),
                blurRadius = 20.dp,
                cornerRadius = 50.dp,
            )
            .clip(shape)
            .background(colors.iceBlue.copy(alpha = 0.22f * alpha))
            .border(1.dp, colors.iceBlue.copy(alpha = 0.55f * alpha), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.cardTitle,
            color = colors.accentBlue.copy(alpha = alpha),
        )
    }
}

@Composable
private fun ShareButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.92f)
            .clip(CircleShape)
            .background(colors.glowSky.copy(alpha = 0.12f))
            .border(0.5.dp, colors.textSecondary, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_share_nodes),
            contentDescription = stringResource(R.string.cd_share),
            tint = colors.textBright,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun ConnectionGrade.labelRes(): Int = when (this) {
    ConnectionGrade.EXCELLENT -> R.string.diag_conn_excellent
    ConnectionGrade.GOOD -> R.string.diag_conn_good
    ConnectionGrade.POOR -> R.string.diag_conn_poor
}
