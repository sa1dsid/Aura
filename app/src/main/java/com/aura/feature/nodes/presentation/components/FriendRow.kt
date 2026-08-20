package com.aura.feature.nodes.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.theme.AuraColors
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.format.formatGrouped
import com.aura.feature.nodes.domain.model.Friend
import com.aura.feature.nodes.domain.model.FriendStatus
import com.aura.feature.nodes.presentation.format.textGlow

private val RowHeight = 68.dp

private val AvatarSize = 36.dp

private val ValueGlowBlur = 6.dp

private val BadgePadding = PaddingValues(start = 8.dp, top = 5.dp, end = 8.dp, bottom = 4.dp)

@Composable
fun FriendRow(
    friend: Friend,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val palette = colors.paletteFor(friend.status)

    AuraCard(modifier = modifier.fillMaxWidth().height(RowHeight), flat = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(initials = friend.initials, palette = palette)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = friend.name,
                            style = AuraTheme.typography.body,
                            color = palette.name,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.handle_format, friend.handle),
                            style = AuraTheme.typography.body,
                            color = palette.handle,
                            maxLines = 1,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Earning(
                            value = friend.spark.formatGrouped(),
                            unit = stringResource(R.string.nodes_unit_spark),
                            palette = palette,
                        )
                        Earning(
                            value = friend.ion
                                .takeIf { it > 0 }
                                ?.formatGrouped()
                                ?: stringResource(R.string.nodes_value_empty),
                            unit = stringResource(R.string.unit_ion),
                            palette = palette,
                        )
                    }
                }
            }

            StatusBadge(status = friend.status, palette = palette)
        }
    }
}

@Composable
private fun Avatar(
    initials: String,
    palette: FriendPalette,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(AuraTheme.colors.glowSky.copy(alpha = 0.08f))
            .border(0.5.dp, palette.avatarBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = AuraTheme.typography.body,
            color = palette.name,
        )
    }
}

@Composable
private fun Earning(
    value: String,
    unit: String,
    palette: FriendPalette,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = AuraTheme.typography.listValue.copy(
                shadow = palette.valueGlow?.let { textGlow(it, ValueGlowBlur) },
            ),
            color = palette.value,
            maxLines = 1,
        )
        Text(
            text = unit,
            style = AuraTheme.typography.cardCaption,
            color = AuraTheme.colors.textSecondary.copy(alpha = 0.45f),
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusBadge(
    status: FriendStatus,
    palette: FriendPalette,
    modifier: Modifier = Modifier,
) {
    val label = when (status) {
        FriendStatus.EARNING -> R.string.nodes_badge_earning
        FriendStatus.SPARK_ONLY -> R.string.nodes_badge_spark_only
        FriendStatus.INACTIVE -> R.string.nodes_badge_inactive
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(palette.badgeFill)
            .border(0.5.dp, palette.badgeBorder, CircleShape)
            .padding(BadgePadding),
    ) {
        Text(
            text = stringResource(label).uppercase(),
            style = AuraTheme.typography.listValue,
            color = palette.badgeText,
            maxLines = 1,
        )
    }
}

private data class FriendPalette(
    val avatarBorder: Color,
    val name: Color,
    val handle: Color,
    val value: Color,
    val valueGlow: Color?,
    val badgeFill: Color,
    val badgeBorder: Color,
    val badgeText: Color,
)

private fun AuraColors.paletteFor(status: FriendStatus): FriendPalette = when (status) {
    FriendStatus.EARNING -> FriendPalette(
        avatarBorder = textSecondary,
        name = textBright,
        handle = textSecondary,
        value = green,
        valueGlow = green.copy(alpha = 0.80f),
        badgeFill = green.copy(alpha = 0.22f),
        badgeBorder = green.copy(alpha = 0.55f),
        badgeText = green,
    )

    FriendStatus.SPARK_ONLY -> FriendPalette(
        avatarBorder = textSecondary,
        name = textBright,
        handle = textSecondary,
        value = textBright,
        valueGlow = null,
        badgeFill = iceBlue.copy(alpha = 0.22f),
        badgeBorder = iceBlue.copy(alpha = 0.55f),
        badgeText = accentBlue,
    )

    FriendStatus.INACTIVE -> FriendPalette(
        avatarBorder = borderMuted,
        name = textDisabled,
        handle = borderMuted,
        value = textDisabled,
        valueGlow = null,
        badgeFill = iceBlue.copy(alpha = 0.22f),
        badgeBorder = iceBlue.copy(alpha = 0.55f),
        badgeText = textDisabled,
    )
}
