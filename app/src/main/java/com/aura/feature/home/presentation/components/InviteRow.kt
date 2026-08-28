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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraPill
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.component.avatarShadows
import com.aura.core.designsystem.component.drawPlanet
import com.aura.core.designsystem.component.leadAvatarShadows
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
            FriendAvatars(filled = invite.activeFriends, total = invite.friendsTarget)

            Spacer(Modifier.width(12.dp))

            Text(
                text = stringResource(
                    R.string.invite_progress,
                    invite.activeFriends,
                    invite.friendsTarget,
                    invite.referralRatePercent,
                ),
                style = AuraTheme.typography.title,
                color = colors.textBright,
                modifier = Modifier.weight(1f),
            )

            AuraPill(
                text = stringResource(R.string.invite_button),
                textStyle = AuraTheme.typography.caption,
                contentColor = colors.accentBlue,
                borderColor = colors.iceBlue.copy(alpha = 0.55f),
                backgroundColor = colors.iceBlue.copy(alpha = 0.22f),
                borderWidth = 0.5.dp,
                horizontalPadding = 12.dp,
                verticalPadding = 6.dp,
                onClick = onInviteClick,
            )
        }
    }
}

private val AvatarCellSize = 44.dp
private const val AvatarBodyFraction = 24f / 44f

private val AvatarStep = 11.dp

private val AvatarRimFilled = 0.2.dp
private val AvatarRimEmpty = 0.5.dp
private val AvatarDash = 6.dp

@Composable
private fun FriendAvatars(filled: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val density = LocalDensity.current
    val dash = remember(density) {
        val length = with(density) { AvatarDash.toPx() }
        PathEffect.dashPathEffect(floatArrayOf(length, length))
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AvatarStep - AvatarCellSize),
    ) {
        repeat(total) { index ->
            val isFilled = index < filled

            Box(
                Modifier
                    .size(AvatarCellSize)
                    .auraGlowLayers(
                        shadows = if (index == 0 && isFilled) {
                            colors.leadAvatarShadows
                        } else {
                            colors.avatarShadows
                        },
                        coreSize = AvatarCellSize * AvatarBodyFraction,
                    )
                    .drawBehind {
                        val bodyRadius = size.minDimension / 2f * AvatarBodyFraction

                        drawCircle(color = colors.surfaceTop, radius = bodyRadius + 1.dp.toPx())

                        drawPlanet(
                            bodyFraction = AvatarBodyFraction,
                            bodyColor = colors.background,
                            dotColor = colors.textTertiary,
                            haloAlpha = 0f,
                        )

                        val rimWidth = if (isFilled) {
                            AvatarRimFilled.toPx()
                        } else {
                            AvatarRimEmpty.toPx()
                        }
                        drawCircle(
                            color = colors.textBright,
                            radius = bodyRadius - rimWidth / 2f,
                            style = Stroke(
                                width = rimWidth,
                                pathEffect = if (isFilled) null else dash,
                            ),
                        )
                    }
            )
        }
    }
}
