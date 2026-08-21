package com.aura.feature.account.presentation.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.theme.AuraTheme

private val DialogShape = RoundedCornerShape(24.dp)

private val PillShape = RoundedCornerShape(percent = 50)

private val ActionCorner = 25.dp

private const val ENTER_MILLIS = 200

private const val EXIT_MILLIS = 150

private const val SCRIM_ALPHA = 0.70f

private const val ENTER_SCALE = 0.94f

@Composable
fun DeleteAccountDialog(
    visible: Boolean,
    onKeepClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val inspecting = LocalInspectionMode.current
    val reveal = remember { Animatable(if (inspecting && visible) 1f else 0f) }

    var rendered by remember { mutableStateOf(inspecting && visible) }

    LaunchedEffect(visible) {
        if (visible) {
            rendered = true
            reveal.animateTo(1f, tween(ENTER_MILLIS, easing = LinearOutSlowInEasing))
            return@LaunchedEffect
        }
        if (!rendered) return@LaunchedEffect
        reveal.animateTo(0f, tween(EXIT_MILLIS, easing = FastOutLinearInEasing))
        rendered = false
    }

    if (!rendered) return

    BackHandler(enabled = visible, onBack = onKeepClick)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(color = colors.background, alpha = SCRIM_ALPHA * reveal.value) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onKeepClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    alpha = reveal.value
                    scaleX = ENTER_SCALE + (1f - ENTER_SCALE) * reveal.value
                    scaleY = scaleX
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DialogShape)
                    .background(Brush.verticalGradient(listOf(colors.sheetTop, colors.sheetBottom)))
                    .border(1.dp, colors.dialogBorder, DialogShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(1.dp)
                    .padding(top = 10.dp, bottom = 16.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(PillShape)
                        .background(colors.sheetMuted)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.menu_delete_title),
                    style = AuraTheme.typography.dialogHeading,
                    color = colors.warning,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.menu_delete_text),
                    style = AuraTheme.typography.dialogBody,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(26.dp))

                DialogAction(
                    text = stringResource(R.string.menu_keep),
                    contentColor = colors.textPrimary,
                    borderColor = colors.accentBlue,
                    glowColor = colors.glowIce,
                    glowRadius = 6.dp,
                    onClick = onKeepClick,
                )

                Spacer(Modifier.height(10.dp))

                DialogAction(
                    text = stringResource(R.string.menu_delete_confirm),
                    contentColor = colors.warning,
                    borderColor = colors.warning,
                    glowColor = colors.warning,
                    glowRadius = 4.dp,
                    onClick = onDeleteClick,
                )
            }

            CloseButton(
                onClick = onKeepClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 17.4.dp, end = 17.2.dp),
            )
        }
    }
}

@Composable
private fun DialogAction(
    text: String,
    contentColor: Color,
    borderColor: Color,
    glowColor: Color,
    glowRadius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .pressScale(interactionSource)
            .auraDropShadow(
                color = glowColor,
                blurRadius = glowRadius,
                cornerRadius = ActionCorner,
            )
            .clip(PillShape)
            .background(colors.sheetTop)
            .border(1.dp, borderColor, PillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.sheetActionLabel,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(AuraTheme.colors.sheetCloseBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Preview(widthDp = 375, heightDp = 815)
@Composable
private fun DeleteAccountDialogPreview() {
    AuraTheme {
        DeleteAccountDialog(visible = true, onKeepClick = {}, onDeleteClick = {})
    }
}
