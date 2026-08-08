package com.aura.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraPill
import com.aura.core.designsystem.component.drawPlanet
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.InviteState

@Composable
fun InviteRow(
    invite: InviteState,
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FriendAvatars(count = invite.friendsTarget)

            Spacer(Modifier.width(12.dp))

            Text(
                text = "${invite.friendsJoined}/${invite.friendsTarget} friends " +
                    "+${invite.referralRatePercent}% rate",
                style = AuraTheme.typography.caption,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )

            AuraPill(
                text = "INVITE",
                contentColor = colors.textPrimary,
                borderColor = colors.borderStrong,
                horizontalPadding = 12.dp,
                verticalPadding = 6.dp,
                onClick = onInviteClick,
            )
        }
    }
}

@Composable
private fun FriendAvatars(count: Int, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-9).dp),
    ) {
        repeat(count) {
            Box(
                Modifier
                    .size(28.dp)
                    .drawBehind {
                        drawCircle(color = colors.background, radius = size.minDimension / 2f * 0.86f)
                        drawPlanet(
                            bodyColor = colors.surfaceElevated,
                            dotColor = colors.mapDotIdle,
                            haloAlpha = 0f,
                        )
                    }
            )
        }
    }
}
