package com.aura.feature.terminal.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraBurgerButton
import com.aura.core.designsystem.component.AuraNewsPlanet
import com.aura.core.designsystem.theme.AuraTheme

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
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AuraBurgerButton(onClick = onMenuClick)

        Text(
            text = stringResource(R.string.terminal_logo),
            style = AuraTheme.typography.logo,
            color = AuraTheme.colors.textBright,
        )

        AuraNewsPlanet(hasUnread = hasUnreadNews, onClick = onNewsClick)
    }
}
