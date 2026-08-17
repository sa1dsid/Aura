package com.aura.feature.account.presentation.menu

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraBottomSheet
import com.aura.core.designsystem.component.AuraOutlinedButton
import com.aura.core.designsystem.component.AuraPrimaryButton
import com.aura.core.designsystem.theme.AuraTheme

@Composable
fun DeleteAccountSheet(
    visible: Boolean,
    onKeepClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraBottomSheet(
        visible = visible,
        onDismissRequest = onKeepClick,
        modifier = modifier,
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.menu_delete_title),
            style = AuraTheme.typography.sheetHeading,
            color = colors.warning,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.menu_delete_text),
            style = AuraTheme.typography.sheetBody,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))

        AuraPrimaryButton(
            text = stringResource(R.string.menu_keep),
            onClick = onKeepClick,
        )

        Spacer(Modifier.height(12.dp))

        AuraOutlinedButton(
            text = stringResource(R.string.menu_delete_confirm),
            onClick = onDeleteClick,
            borderColor = colors.warning,
            contentColor = colors.warning,
        )
    }
}

@Preview(widthDp = 375, heightDp = 815)
@Composable
private fun DeleteAccountSheetPreview() {
    AuraTheme {
        DeleteAccountSheet(visible = true, onKeepClick = {}, onDeleteClick = {})
    }
}
