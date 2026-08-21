package com.aura.feature.nodes.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.auraDropShadows
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.theme.AuraColors
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.nodes.domain.model.Friend
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.presentation.format.squadShadows

private val FrameShape = RoundedCornerShape(16.dp)

private val DotSize = 6.dp

@Composable
fun ActiveSquadCard(
    tier: ReferralTier,
    activeFriends: Int,
    squad: List<Friend>,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val tierName = stringResource(tier.labelRes)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.nodes_squad_title, tierName).uppercase(),
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(rememberFrameGlow(colors))
                .clip(FrameShape)
                .background(
                    Brush.verticalGradient(listOf(colors.surfaceTop, colors.surfaceBottom))
                )
                .border(0.5.dp, colors.green, FrameShape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(DotSize)
                        .auraGlowLayers(colors.squadShadows)
                        .clip(CircleShape)
                        .background(colors.green)
                )
                Text(
                    text = stringResource(R.string.nodes_squad_active, activeFriends),
                    style = AuraTheme.typography.cardLabel,
                    color = colors.green,
                )
                Text(
                    text = stringResource(R.string.nodes_squad_separator),
                    style = AuraTheme.typography.cardLabel,
                    color = colors.green,
                )
                Text(
                    text = stringResource(R.string.nodes_squad_unlocked, tierName).uppercase(),
                    style = AuraTheme.typography.cardLabel,
                    color = colors.green,
                )
            }

            squad.forEach { friend -> FriendRow(friend = friend) }
        }
    }
}

@Composable
private fun rememberFrameGlow(colors: AuraColors): Modifier = remember(colors) {
    Modifier.auraDropShadows(colors.squadShadows, cornerRadius = 16.dp)
}
