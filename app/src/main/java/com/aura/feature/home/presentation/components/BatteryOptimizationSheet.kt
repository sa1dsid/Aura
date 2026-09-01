package com.aura.feature.home.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraBottomSheet
import com.aura.core.designsystem.component.AuraSheetCloseButton
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme

private val IconSize = 48.dp

private val PillShape = RoundedCornerShape(percent = 50)

private val ActionHeight = 44.dp

private val ActionIconSize = 16.dp

private const val PILL_FILL_ALPHA = 0.22f

private const val ACTION_BORDER_ALPHA = 0.60f

@Composable
fun BatteryOptimizationSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onDisableClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraBottomSheet(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxWidth()) {
            AuraSheetCloseButton(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        Spacer(Modifier.height(20.dp))

        Image(
            painter = painterResource(R.drawable.ic_battery_charging),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(IconSize),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.battery_sheet_title),
            style = AuraTheme.typography.sheetHeading,
            color = colors.textBright,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.battery_sheet_text),
            style = AuraTheme.typography.cardCaption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(24.dp))

        DisableAction(onClick = onDisableClick)
    }
}

@Composable
private fun ColumnScope.DisableAction(onClick: () -> Unit) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionHeight)
            .pressScale(pressed = isPressed)
            .clip(PillShape)
            .background(colors.iceBlue.copy(alpha = PILL_FILL_ALPHA))
            .border(1.dp, colors.iceBlue.copy(alpha = ACTION_BORDER_ALPHA), PillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.battery_sheet_action),
            style = AuraTheme.typography.sheetActionLabel,
            color = colors.textBright,
        )

        Image(
            painter = painterResource(R.drawable.ic_export),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(ActionIconSize),
        )
    }
}

@Preview(widthDp = 375, heightDp = 320, backgroundColor = 0xFF030507, showBackground = true)
@Composable
private fun BatteryOptimizationSheetPreview() {
    AuraTheme {
        BatteryOptimizationSheet(visible = true, onDismissRequest = {}, onDisableClick = {})
    }
}
