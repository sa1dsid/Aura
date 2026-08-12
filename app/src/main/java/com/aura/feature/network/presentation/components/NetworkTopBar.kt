package com.aura.feature.network.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun NetworkTopBar(
    handle: String?,
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
            text = handle?.let { stringResource(R.string.net_handle, it) }.orEmpty(),
            style = AuraTheme.typography.cardTitle,
            color = AuraTheme.colors.textPrimary,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )

        AuraNewsPlanet(hasUnread = hasUnreadNews, onClick = onNewsClick)
    }
}
