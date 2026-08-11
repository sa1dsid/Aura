package com.aura.feature.onboarding.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.PRESS_FADE_MILLIS
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.domain.model.INVITE_CODE_LENGTH

private val FieldShape = RoundedCornerShape(16.dp)

private val ChipShape = RoundedCornerShape(10.dp)

@Composable
fun InviteCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    onPaste: () -> Unit,
    locked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(FieldShape)
            .background(colors.authSurface)
            .border(1.dp, colors.authBorder, FieldShape)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(25.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = code,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isLetterOrDigit() }
                        .uppercase()
                        .take(INVITE_CODE_LENGTH)
                    onCodeChange(filtered)
                },
                singleLine = true,
                readOnly = locked,
                textStyle = AuraTheme.typography.inviteCode.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.textPrimary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (code.isEmpty()) {
                        Text(
                            text = stringResource(R.string.invite_code_placeholder),
                            style = AuraTheme.typography.inviteCode,
                            color = colors.authCodePlaceholder,
                        )
                    }
                    innerTextField()
                },
            )
        }

        if (locked) {
            Box(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.invite_applied_hint),
                    style = AuraTheme.typography.chipLabel,
                    color = colors.authTextMuted,
                )
            }
        } else {
            PasteChip(onClick = onPaste)
        }
    }
}

@Composable
private fun PasteChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val background by animateColorAsState(
        targetValue = if (isPressed) colors.authPasteBorder else colors.authSegmentActive,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "paste-chip-background",
    )

    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(background)
            .border(1.dp, colors.authPasteBorder, ChipShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.invite_paste),
            style = AuraTheme.typography.chipLabel,
            color = Color.White,
        )
    }
}
