package com.aura.feature.ioni.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.theme.AuraTheme

private val PillShape = RoundedCornerShape(32.dp)

private val SendSize = 42.dp

private val SendIconSize = 20.dp

private val SupportIconBox = 32.dp

private val SupportIconSize = 20.dp

private const val CHIP_TEXT_ALPHA = 0.80f

private const val HINT_ALPHA = 0.32f

private const val DISABLED_ALPHA = 0.40f

private const val FADE_MILLIS = 200

@Composable
fun IoniQuestionChips(
    questions: List<String>,
    onQuestionClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        itemsIndexed(questions) { index, question ->
            Chip(text = question, onClick = { onQuestionClick(index) })
        }
    }
}

@Composable
private fun Chip(
    text: String,
    onClick: () -> Unit,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .pressScale(interactionSource)
            .clip(PillShape)
            .background(colors.ioniPanel)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.title,
            color = colors.textPrimary.copy(alpha = CHIP_TEXT_ALPHA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun IoniSupportPill(
    email: String,
    isExpanded: Boolean,
    onSupportClick: () -> Unit,
    onEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .pressScale(interactionSource)
            .clip(PillShape)
            .background(colors.ioniPanel)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = if (isExpanded) onEmailClick else onSupportClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(SupportIconBox)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sms),
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(SupportIconSize),
            )
        }

        Text(
            text = if (isExpanded) email else stringResource(R.string.ioni_support),
            style = AuraTheme.typography.title,
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
fun IoniInputField(
    value: String,
    isSendEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val sendInteraction = remember { MutableInteractionSource() }

    val sendAlpha by animateFloatAsState(
        targetValue = if (isSendEnabled) 1f else DISABLED_ALPHA,
        animationSpec = tween(FADE_MILLIS),
        label = "ioni-send-alpha",
    )
    val hintColor by animateColorAsState(
        targetValue = colors.textPrimary.copy(alpha = HINT_ALPHA),
        animationSpec = tween(FADE_MILLIS),
        label = "ioni-hint",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(colors.ioniPanel)
            .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.ioni_input_hint),
                    style = AuraTheme.typography.actionLabel,
                    color = hintColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = AuraTheme.typography.actionLabel.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.textBright),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (isSendEnabled) onSendClick() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(
            modifier = Modifier
                .alpha(sendAlpha)
                .pressScale(sendInteraction, enabled = isSendEnabled)
                .size(SendSize)
                .clip(CircleShape)
                .background(colors.textPrimary)
                .clickable(
                    interactionSource = sendInteraction,
                    indication = null,
                    enabled = isSendEnabled,
                    onClick = onSendClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_up),
                contentDescription = stringResource(R.string.cd_ioni_send),
                tint = colors.background,
                modifier = Modifier.size(SendIconSize),
            )
        }
    }
}
