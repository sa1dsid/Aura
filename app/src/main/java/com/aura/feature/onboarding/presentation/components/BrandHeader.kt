package com.aura.feature.onboarding.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.theme.AuraTheme

@Composable
fun BrandLogoRow(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "I",
            style = AuraTheme.typography.brandTitle,
            color = colors.textPrimary,
        )
        Image(
            painter = painterResource(R.drawable.img_globe),
            contentDescription = null,
            modifier = Modifier.size(38.dp),
        )
        Text(
            text = "Aura",
            style = AuraTheme.typography.brandTitle,
            color = colors.textPrimary,
        )
    }
}

@Composable
fun BrandTagline(modifier: Modifier = Modifier) {
    Text(
        text = "In the beginning was the ION",
        style = AuraTheme.typography.brandTagline,
        color = AuraTheme.colors.authTextMuted,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
