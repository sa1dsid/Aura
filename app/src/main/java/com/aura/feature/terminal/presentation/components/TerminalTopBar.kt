package com.aura.feature.terminal.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraBurgerButton
import com.aura.core.designsystem.component.AuraNewsPlanet
import com.aura.core.designsystem.theme.AuraTheme

private val BarHeight = 60.dp

private val BarPadding = 16.dp

private val LogoWidth = 59.8.dp

private val LogoHeight = 40.dp

@Composable
fun TerminalTopBar(
    hasUnreadNews: Boolean,
    onMenuClick: () -> Unit,
    onNewsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = BarPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AuraBurgerButton(onClick = onMenuClick)

        Icon(
            painter = painterResource(R.drawable.ic_logo_io),
            contentDescription = stringResource(R.string.cd_logo),
            tint = AuraTheme.colors.textIce,
            modifier = Modifier.size(width = LogoWidth, height = LogoHeight),
        )

        AuraNewsPlanet(hasUnread = hasUnreadNews, onClick = onNewsClick)
    }
}
