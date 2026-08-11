package com.aura.feature.onboarding.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.aura.core.designsystem.theme.AuraTheme

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.authBorder)
        )
        Text(
            text = stringResource(R.string.auth_or),
            style = AuraTheme.typography.segmentLabel,
            color = colors.authTextDim,
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.authBorder)
        )
    }
}
