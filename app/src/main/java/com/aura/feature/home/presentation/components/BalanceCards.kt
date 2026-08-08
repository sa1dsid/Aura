package com.aura.feature.home.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraCard
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BalanceCard(
            label = "ACCRUED",
            amount = balances.accrued,
            showLiveDot = true,
            modifier = Modifier.weight(1f),
        )
        BalanceCard(
            label = "AVAILABLE TO WITHDRAW",
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
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(
                text = label,
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = amount.formatGrouped(),
                    style = AuraTheme.typography.displayNumber,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "ION",
                    style = AuraTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
                if (showLiveDot) {
                    Spacer(Modifier.width(5.dp))
                    Box(
                        Modifier
                            .padding(bottom = 6.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.accentBlue)
                    )
                }
            }
        }
    }
}
