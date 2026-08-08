package com.aura.feature.home.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.HomeTab

private const val TAB_FADE_MILLIS = 200

@Composable
fun AuraBottomBar(
    selected: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabItem(
                tab = HomeTab.HOME,
                iconRes = R.drawable.ic_home,
                isSelected = selected == HomeTab.HOME,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            TabItem(
                tab = HomeTab.NODES,
                iconRes = R.drawable.ic_users,
                isSelected = selected == HomeTab.NODES,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            IoniTab(
                isSelected = selected == HomeTab.IONI,
                onClick = { onTabSelected(HomeTab.IONI) },
                modifier = Modifier.weight(1f),
            )
            TabItem(
                tab = HomeTab.TERMINAL,
                iconRes = R.drawable.ic_wallet,
                isSelected = selected == HomeTab.TERMINAL,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            TabItem(
                tab = HomeTab.NETWORK,
                iconRes = R.drawable.ic_wifi,
                isSelected = selected == HomeTab.NETWORK,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: HomeTab,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val tint by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.textTertiary,
        animationSpec = tween(TAB_FADE_MILLIS),
        label = "tab-tint",
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(TAB_FADE_MILLIS),
        label = "tab-border",
    )

    Column(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.9f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onClick(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .border(
                    width = 1.dp,
                    color = colors.borderStrong.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(9.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = tab.label,
            style = AuraTheme.typography.navLabel,
            color = tint,
        )
    }
}

@Composable
private fun IoniTab(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.borderStrong,
        animationSpec = tween(TAB_FADE_MILLIS),
        label = "ioni-border",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .pressScale(interactionSource, pressedScale = 0.92f)
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = HomeTab.IONI.label,
                style = AuraTheme.typography.badge,
                color = colors.textPrimary,
            )
        }
    }
}
