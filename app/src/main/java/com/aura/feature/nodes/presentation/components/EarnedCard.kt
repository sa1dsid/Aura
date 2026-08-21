package com.aura.feature.nodes.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.format.StyledArg
import com.aura.feature.home.presentation.format.annotatedFormat
import com.aura.feature.home.presentation.format.formatGrouped
import com.aura.feature.home.presentation.format.formatRate
import com.aura.feature.nodes.domain.model.ReferralRewards
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.domain.model.TierRates
import com.aura.feature.nodes.presentation.format.textGlow

private val ColumnWidth = 74.dp

private val ValueGlowBlur = 6.dp

@Composable
fun EarnedCard(
    rewards: ReferralRewards,
    tier: ReferralTier,
    rates: TierRates,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.nodes_earned).uppercase(),
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardColumn(
                    value = rewards.spark.formatGrouped(),
                    valueColor = colors.textBright,
                    unit = stringResource(R.string.nodes_unit_spark),
                )
                RewardColumn(
                    value = rewards.ion.formatGrouped(),
                    valueColor = colors.green,
                    unit = stringResource(R.string.unit_ion),
                    glow = true,
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            Text(
                text = annotatedFormat(
                    template = stringResource(R.string.nodes_tier_line),
                    StyledArg(
                        text = stringResource(tier.labelRes),
                        style = SpanStyle(
                            color = colors.accentBlue,
                            fontWeight = FontWeight.Bold,
                        ),
                    ),
                    StyledArg(text = rates.sparkPercent.toString()),
                    StyledArg(text = rates.withdrawalPercent.formatRate()),
                ),
                style = AuraTheme.typography.cardNote,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun RewardColumn(
    value: String,
    valueColor: Color,
    unit: String,
    modifier: Modifier = Modifier,
    glow: Boolean = false,
) {
    val colors = AuraTheme.colors
    val bonus = stringResource(R.string.nodes_unit_bonus)

    Column(
        modifier = modifier.widthIn(min = ColumnWidth),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = AuraTheme.typography.dataValue.copy(
                shadow = if (glow) {
                    textGlow(colors.green.copy(alpha = 0.80f), ValueGlowBlur)
                } else {
                    null
                },
            ),
            color = valueColor,
            maxLines = 1,
        )

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(unit) }
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Light,
                        color = colors.textSecondary.copy(alpha = 0.45f),
                    )
                ) {
                    append(" ")
                    append(bonus)
                }
            },
            style = AuraTheme.typography.cardUnit,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}
