package com.aura.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R
import com.aura.core.designsystem.component.AuraBottomSheet
import com.aura.core.designsystem.component.activeDotShadows
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.auraDropShadows
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.BONUS_TOTAL_STEPS
import com.aura.feature.home.domain.model.BonusStep
import com.aura.feature.home.domain.model.BonusStepPage
import com.aura.feature.home.domain.model.BonusWithdrawalTeaser
import com.aura.feature.home.presentation.format.StyledArg
import com.aura.feature.home.presentation.format.annotatedFormat

private val EyebrowGap = 4.5.dp

private val TitleGap = 7.5.dp

private val DotsGap = 19.dp

private val DotSize = 6.dp

private val DotSpacing = 6.dp

private val RingBox = 106.dp

private val RingStroke = 6.dp

private val RingValueGap = 2.5.dp

private val RingBodyGap = 20.dp

private val BodyActionGap = 15.dp

private val ActionHintGap = 9.dp

private val ActionWidth = 267.dp

private val ActionHeight = 44.dp

private val ActionGlowBlur = 8.dp

private val PillGap = 16.dp

private val PillWidth = 193.dp

private val PillHeight = 24.dp

private val PillCorner = 9.5.dp

private val PillBodyGap = 18.dp

private val DoneMarkSize = 76.dp

private val DoneMarkBorder = 2.dp

private val DoneTitleGap = 12.dp

private val DoneRatioGap = 2.dp

private val DoneDotsGap = 14.dp

private val DoneBodyGap = 15.dp

private val DoneActionGap = 17.dp

private val CheckStroke = 3.dp

private val RingValueSize = 18.sp

private val RingValueLineHeight = 23.7.sp

private const val ACTION_FILL_ALPHA = 0.22f

private const val ACTION_BORDER_ALPHA = 0.60f

private const val ACTION_GLOW_ALPHA = 0.60f

private const val DOT_GLOW_ALPHA = 0.70f

private val DotGlowBlur = 6.dp

private val BodyWidth = 240.dp

private val ActionShape = RoundedCornerShape(percent = 50)

@Composable
fun BonusStepsSheet(
    teaser: BonusWithdrawalTeaser?,
    onDismissRequest: () -> Unit,
    onStartTapping: () -> Unit,
    onShareInvite: () -> Unit,
    onCongratulationSeen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = teaser?.openingPage()

    AuraBottomSheet(
        visible = teaser != null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        when (page) {
            is BonusStepPage.Done -> DoneBody(page = page, onNextClick = onCongratulationSeen)

            is BonusStepPage.Task -> TaskBody(
                page = page,
                onDismissRequest = onDismissRequest,
                onStartTapping = onStartTapping,
                onShareInvite = onShareInvite,
            )

            null -> Unit
        }
    }
}

@Composable
private fun ColumnScope.TaskBody(
    page: BonusStepPage.Task,
    onDismissRequest: () -> Unit,
    onStartTapping: () -> Unit,
    onShareInvite: () -> Unit,
) {
    val step = page.step

    Eyebrow(step = step)

    Spacer(Modifier.height(EyebrowGap))

    Title(text = stringResource(step.titleRes(), step.target))

    Spacer(Modifier.height(TitleGap))

    StepDots(completed = page.completedSteps, current = page.completedSteps)

    if (page.isLocked) {
        Spacer(Modifier.height(PillGap))

        SoonPill()

        Spacer(Modifier.height(PillBodyGap))
    } else {
        Spacer(Modifier.height(DotsGap))

        ProgressRing(current = page.current, target = step.target, unitRes = step.unitRes())

        Spacer(Modifier.height(RingBodyGap))
    }

    BodyText(step.body())

    Spacer(Modifier.height(BodyActionGap))

    PrimaryAction(
        text = stringResource(step.actionRes(page.current)),
        onClick = when (step) {
            BonusStep.NETWORK_SYNC -> onShareInvite
            BonusStep.SIGNAL_LOCK -> onStartTapping
            else -> onDismissRequest
        },
    )

    when (step) {
        BonusStep.SIGNAL_LOCK -> {
            Spacer(Modifier.height(ActionHintGap))
            Hint(stringResource(R.string.bonus_step_reveal_hint), onClick = null)
        }

        BonusStep.NETWORK_SYNC -> {
            Spacer(Modifier.height(ActionHintGap))
            Hint(stringResource(R.string.bonus_step_action_got_it), onClick = onDismissRequest)
        }

        else -> Unit
    }
}

@Composable
private fun ColumnScope.DoneBody(page: BonusStepPage.Done, onNextClick: () -> Unit) {
    val colors = AuraTheme.colors
    val step = page.step

    DoneMark()

    Spacer(Modifier.height(DoneTitleGap))

    Title(text = stringResource(R.string.bonus_step_done_title, stringResource(step.nameRes())))

    Spacer(Modifier.height(DoneRatioGap))

    Text(
        text = stringResource(
            R.string.bonus_step_done_ratio,
            step.target,
            step.target,
            stringResource(step.unitRes()),
        ),
        style = AuraTheme.typography.latestValue,
        color = colors.accentBlue,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(DoneDotsGap))

    StepDots(completed = page.completedSteps, current = page.completedSteps)

    Spacer(Modifier.height(DoneBodyGap))

    BodyText(
        listOf(
            AnnotatedString(
                stringResource(R.string.bonus_step_done_body_1, page.completedSteps + 1)
            ),
            AnnotatedString(stringResource(R.string.bonus_step_done_body_2)),
        )
    )

    Spacer(Modifier.height(DoneActionGap))

    PrimaryAction(text = stringResource(R.string.bonus_step_action_next), onClick = onNextClick)
}

@Composable
private fun Eyebrow(step: BonusStep) {
    Text(
        text = stringResource(
            R.string.bonus_step_eyebrow,
            step.ordinal + 1,
            BONUS_TOTAL_STEPS,
            stringResource(step.capsRes()),
        ),
        style = AuraTheme.typography.caption,
        color = AuraTheme.colors.accentBlue,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = AuraTheme.typography.sheetHeading,
        color = AuraTheme.colors.textBright,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BodyText(lines: List<AnnotatedString>) {
    Column(
        modifier = Modifier.widthIn(max = BodyWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                style = AuraTheme.typography.body,
                color = AuraTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StepDots(completed: Int, current: Int) {
    val colors = AuraTheme.colors

    Row(horizontalArrangement = Arrangement.spacedBy(DotSpacing)) {
        repeat(BONUS_TOTAL_STEPS) { index ->
            val color = when {
                index < completed -> colors.green
                index == current -> colors.accentBlue
                else -> colors.stepDotIdle
            }
            val lit = index <= current

            Box(
                Modifier
                    .size(DotSize)
                    .then(
                        if (lit) {
                            Modifier.auraGlow(
                                color = colors.bonusBadgeGlow.copy(alpha = DOT_GLOW_ALPHA),
                                width = DotSize,
                                height = DotSize,
                                blurRadius = DotGlowBlur,
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun ProgressRing(current: Int, target: Int, unitRes: Int) {
    val colors = AuraTheme.colors
    val ringColor = if (current > 0) colors.bonusBadgeGlow else colors.borderMuted
    val trackColor = colors.authBorder

    Box(
        modifier = Modifier
            .size(RingBox)
            .drawBehind {
                val stroke = RingStroke.toPx()
                drawCircle(
                    color = trackColor,
                    radius = (size.minDimension - stroke) / 2f,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawCircle(
                    color = ringColor,
                    radius = (size.minDimension - stroke) / 2f,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.bonus_step_ratio, current, target),
                style = AuraTheme.typography.latestValue.copy(
                    fontSize = RingValueSize,
                    lineHeight = RingValueLineHeight,
                ),
                color = colors.textBright,
            )

            Spacer(Modifier.height(RingValueGap))

            Text(
                text = stringResource(unitRes),
                style = AuraTheme.typography.caption,
                color = colors.authTextDim,
            )
        }
    }
}

@Composable
private fun SoonPill() {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(PillCorner)

    Box(
        modifier = Modifier
            .size(width = PillWidth, height = PillHeight)
            .border(1.dp, colors.borderMuted, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.bonus_step_data_share_pill),
            style = AuraTheme.typography.caption,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun DoneMark() {
    val colors = AuraTheme.colors

    Box(
        modifier = Modifier
            .size(DoneMarkSize)
            .auraDropShadows(colors.activeDotShadows, DoneMarkSize / 2, outsideOnly = true)
            .border(DoneMarkBorder, colors.green, CircleShape)
            .drawBehind {
                val stroke = CheckStroke.toPx()
                val w = size.width
                drawLine(
                    color = colors.green,
                    start = Offset(w * 0.368f, w * 0.482f),
                    end = Offset(w * 0.467f, w * 0.581f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.green,
                    start = Offset(w * 0.467f, w * 0.581f),
                    end = Offset(w * 0.632f, w * 0.383f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
    )
}

@Composable
private fun ColumnScope.PrimaryAction(text: String, onClick: () -> Unit) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .pressScale(pressed = isPressed)
            .size(width = ActionWidth, height = ActionHeight)
            .auraDropShadow(
                color = colors.glowIce.copy(alpha = ACTION_GLOW_ALPHA),
                blurRadius = ActionGlowBlur,
                cornerRadius = ActionHeight / 2,
                outsideOnly = true,
            )
            .clip(ActionShape)
            .background(colors.iceBlue.copy(alpha = ACTION_FILL_ALPHA))
            .border(1.dp, colors.iceBlue.copy(alpha = ACTION_BORDER_ALPHA), ActionShape)
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
            color = colors.textBright,
        )
    }
}

@Composable
private fun ColumnScope.Hint(text: String, onClick: (() -> Unit)?) {
    val interactionSource = remember { MutableInteractionSource() }

    Text(
        text = text,
        style = AuraTheme.typography.cardCaption,
        color = AuraTheme.colors.textDisabled,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                }
            ),
    )
}

@Composable
private fun BonusStep.body(): List<AnnotatedString> {
    val accent = SpanStyle(
        color = AuraTheme.colors.textBright,
        fontWeight = FontWeight.Bold,
    )

    return listOf(
        when (this) {
            BonusStep.SIGNAL_LOCK -> annotatedFormat(
                stringResource(R.string.bonus_step_signal_lock_body),
                StyledArg(stringResource(R.string.bonus_step_signal_lock_accent), accent),
            )

            BonusStep.NETWORK_SYNC -> annotatedFormat(
                stringResource(R.string.bonus_step_network_sync_body),
                StyledArg(stringResource(R.string.bonus_step_network_sync_accent_screen), accent),
                StyledArg(stringResource(R.string.bonus_step_network_sync_accent_tier), accent),
            )

            BonusStep.FULL_UPLINK -> annotatedFormat(
                stringResource(R.string.bonus_step_full_uplink_body),
                StyledArg(stringResource(R.string.bonus_step_full_uplink_accent), accent),
            )

            BonusStep.DATA_SHARE -> annotatedFormat(
                stringResource(R.string.bonus_step_data_share_body),
                StyledArg(stringResource(R.string.bonus_step_data_share_accent), accent),
            )
        }
    )
}

private fun BonusStep.capsRes(): Int = when (this) {
    BonusStep.SIGNAL_LOCK -> R.string.bonus_step_signal_lock_caps
    BonusStep.NETWORK_SYNC -> R.string.bonus_step_network_sync_caps
    BonusStep.FULL_UPLINK -> R.string.bonus_step_full_uplink_caps
    BonusStep.DATA_SHARE -> R.string.bonus_step_data_share_caps
}

private fun BonusStep.nameRes(): Int = when (this) {
    BonusStep.SIGNAL_LOCK -> R.string.bonus_step_signal_lock
    BonusStep.NETWORK_SYNC -> R.string.bonus_step_network_sync
    BonusStep.FULL_UPLINK -> R.string.bonus_step_full_uplink
    BonusStep.DATA_SHARE -> R.string.bonus_step_data_share
}

private fun BonusStep.titleRes(): Int = when (this) {
    BonusStep.SIGNAL_LOCK -> R.string.bonus_step_signal_lock_title
    BonusStep.NETWORK_SYNC -> R.string.bonus_step_network_sync_title
    BonusStep.FULL_UPLINK -> R.string.bonus_step_full_uplink_title
    BonusStep.DATA_SHARE -> R.string.bonus_step_data_share_title
}

private fun BonusStep.unitRes(): Int = when (this) {
    BonusStep.SIGNAL_LOCK -> R.string.bonus_step_unit_taps
    BonusStep.NETWORK_SYNC -> R.string.bonus_step_unit_friends
    BonusStep.FULL_UPLINK -> R.string.bonus_step_unit_days
    BonusStep.DATA_SHARE -> R.string.bonus_step_unit_days
}

private fun BonusStep.actionRes(current: Int): Int = when {
    this == BonusStep.NETWORK_SYNC -> R.string.bonus_step_action_share
    this == BonusStep.SIGNAL_LOCK && current == 0 -> R.string.bonus_step_action_start
    else -> R.string.bonus_step_action_got_it
}

@Preview
@Composable
private fun BonusStepsSheetPreview() {
    AuraTheme {
        BonusStepsSheet(
            teaser = BonusWithdrawalTeaser(
                completedSteps = 0,
                totalSteps = BONUS_TOTAL_STEPS,
                signalLockTaps = 3,
            ),
            onDismissRequest = {},
            onStartTapping = {},
            onShareInvite = {},
            onCongratulationSeen = {},
        )
    }
}
