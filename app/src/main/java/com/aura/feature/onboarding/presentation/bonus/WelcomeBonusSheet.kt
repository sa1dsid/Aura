package com.aura.feature.onboarding.presentation.bonus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.presentation.components.designBottomGap

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

private val ActionShape = RoundedCornerShape(percent = 50)

@Composable
fun WelcomeBonusRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeBonusViewModel = hiltViewModel(),
) {
    val bonusIon by viewModel.bonusIon.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.dismissed.collect { onFinished() }
    }

    WelcomeBonusSheet(
        bonusIon = bonusIon,
        onDismiss = viewModel::onDismiss,
        modifier = modifier,
    )
}

@Composable
fun WelcomeBonusSheet(
    bonusIon: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val scrimInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = scrimInteractionSource,
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SheetShape)
                .background(
                    Brush.verticalGradient(listOf(colors.sheetTop, colors.sheetBottom))
                )
                .border(0.5.dp, colors.sheetBorder, SheetShape)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.15f),
                            1f to Color.Transparent,
                        ),
                        size = size.copy(height = 1.dp.toPx()),
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(top = 10.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = designBottomGap(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(ActionShape)
                    .background(colors.sheetMuted)
            )

            Spacer(Modifier.height(14.dp))

            CloseButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            )

            BonusBadge(bonusIon = bonusIon)

            Spacer(Modifier.height(22.dp))

            Text(
                text = stringResource(R.string.bonus_title),
                style = AuraTheme.typography.sheetHeading,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = bonusText(),
                style = AuraTheme.typography.sheetBody,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
            )

            Spacer(Modifier.height(37.dp))

            PrimaryAction(
                text = stringResource(R.string.bonus_cta),
                onClick = onDismiss,
            )

            Spacer(Modifier.height(12.dp))

            SecondaryAction(
                text = stringResource(R.string.bonus_later),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun bonusText(): AnnotatedString {
    val colors = AuraTheme.colors
    val text = stringResource(R.string.bonus_text)
    val accent = stringResource(R.string.bonus_text_accent)
    val start = text.indexOf(accent)

    return buildAnnotatedString {
        append(text)
        if (start >= 0) {
            addStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = colors.textBright,
                ),
                start = start,
                end = start + accent.length,
            )
        }
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(colors.sheetCloseBackground)
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

@Composable
private fun BonusBadge(
    bonusIon: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .size(120.dp)
            .auraGlow(
                color = colors.bonusBadgeGlow.copy(alpha = 0.45f),
                width = 120.dp,
                height = 120.dp,
                blurRadius = 34.dp,
            )
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(colors.sheetTop, colors.sheetBottom)))
            .drawWithContent {
                drawContent()
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.55f to Color.Transparent,
                            1f to colors.bonusBadgeGlow.copy(alpha = 0.12f),
                        ),
                        center = center,
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                    center = center,
                )
            }
            .border(2.dp, colors.bonusBadgeBorder, CircleShape),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = bonusIon.toString(),
            style = AuraTheme.typography.bonusValue,
            color = colors.authSegmentActiveText,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = stringResource(R.string.unit_ion),
            style = AuraTheme.typography.bonusUnit,
            color = colors.accentBlue,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth(BUTTON_WIDTH_FRACTION)
            .height(50.dp)
            .auraDropShadow(
                color = colors.bonusActionGlow,
                blurRadius = 6.dp,
                cornerRadius = 25.dp,
            )
            .clip(ActionShape)
            .background(colors.sheetTop)
            .border(1.dp, colors.accentBlue, ActionShape)
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
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Text(
        text = text,
        style = AuraTheme.typography.segmentLabel,
        color = AuraTheme.colors.sheetMuted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
}

private const val BUTTON_WIDTH_FRACTION = 268.2f / 327f

@Preview(widthDp = 375, heightDp = 813)
@Composable
private fun WelcomeBonusSheetPreview() {
    AuraTheme {
        WelcomeBonusSheet(bonusIon = 3_000, onDismiss = {})
    }
}
