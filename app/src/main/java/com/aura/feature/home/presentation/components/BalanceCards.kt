package com.aura.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.component.signalDotShadows
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.IonBalances
import com.aura.feature.home.presentation.format.formatGrouped

@Composable
fun BalanceCardsRow(
    balances: IonBalances,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BalanceCard(
            label = stringResource(R.string.home_accrued),
            amount = balances.accrued,
            showLiveDot = true,
            modifier = Modifier.weight(1f),
        )
        BalanceCard(
            label = stringResource(R.string.home_available_withdraw),
            amount = balances.availableToWithdraw,
            showLiveDot = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BalanceCard(
    label: String,
    amount: Long,
    showLiveDot: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = amount.formatGrouped(),
                    style = AuraTheme.typography.displayNumber,
                    color = colors.textBright,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.unit_ion),
                        style = AuraTheme.typography.unitLabel,
                        color = colors.textSecondary,
                    )
                    if (showLiveDot) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .auraGlowLayers(colors.signalDotShadows)
                                .clip(CircleShape)
                                .background(colors.accentBlue)
                        )
                    }
                }
            }
        }
    }
}
