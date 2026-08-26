package com.aura.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraPill
import com.aura.core.designsystem.component.activeDotShadows
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.IoniCard
import com.aura.feature.home.domain.model.IoniState
import com.aura.feature.home.domain.model.chargeFraction
import com.aura.feature.home.domain.model.chargeHours
import com.aura.feature.home.domain.model.chargeLeft
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val IconBoxSize = 32.dp

private val IconSize = 24.dp

private val ArrowBoxSize = 48.dp

private val RowMinHeight = 48.dp

private val ChargeHeight = 1.dp

private val ChargeShape = RoundedCornerShape(percent = 50)

private val BadgeLetterSpacing = 0.08.sp

private const val DIMMED_ALPHA = 0.55f

private const val BADGE_FILL_ALPHA = 0.22f

private const val BADGE_BORDER_ALPHA = 0.55f

private val CHARGE_TICK = 1.minutes

enum class IoniCardMode { SOON, IDLE, POWERED, BLOCKED }

private data class StatusSegment(val text: String, val color: Color)

@Composable
fun IoniAiCard(
    card: IoniCard,
    isBatteryOptimizationDisabled: Boolean,
    onOpenSheet: (IoniSheetKind) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chargeLeft by rememberChargeLeft(card)
    val mode = ioniCardMode(card, chargeLeft, isBatteryOptimizationDisabled)

    when (mode) {
        IoniCardMode.SOON -> SoonCard(
            onClick = { onOpenSheet(card.state.sheetKind()) },
            modifier = modifier,
        )

        IoniCardMode.IDLE -> StatusCard(
            segments = idleSegments(),
            dotColor = AuraTheme.colors.textSecondary,
            dotGlowing = false,
            charge = 0f,
            onClick = { onOpenSheet(IoniSheetKind.IDLE) },
            modifier = modifier,
        )

        IoniCardMode.POWERED -> StatusCard(
            segments = poweredSegments(chargeLeft.chargeHours()),
            dotColor = AuraTheme.colors.green,
            dotGlowing = true,
            charge = chargeLeft.chargeFraction(),
            onClick = { onOpenSheet(IoniSheetKind.LIVE) },
            modifier = modifier,
        )

        IoniCardMode.BLOCKED -> BlockedCard(
            charge = chargeLeft.chargeFraction(),
            onSettingsClick = onSettingsClick,
            modifier = modifier,
        )
    }
}

private fun IoniState.sheetKind(): IoniSheetKind =
    if (this == IoniState.GEO_BLOCKED) IoniSheetKind.GEO else IoniSheetKind.COMING

fun ioniCardMode(
    card: IoniCard,
    chargeLeft: Duration,
    isBatteryOptimizationDisabled: Boolean,
): IoniCardMode = when {
    card.state != IoniState.ACTIVE -> IoniCardMode.SOON
    chargeLeft <= Duration.ZERO -> IoniCardMode.IDLE
    !isBatteryOptimizationDisabled -> IoniCardMode.BLOCKED
    else -> IoniCardMode.POWERED
}

@Composable
private fun SoonCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    CardShell(
        onClick = onClick,
        modifier = modifier.alpha(DIMMED_ALPHA),
        trailing = {
            Badge(
                text = stringResource(R.string.ioni_badge_soon),
                color = colors.iceBlue,
                contentColor = colors.accentBlue,
            )
        },
    ) {
        Title()
    }
}

@Composable
private fun StatusCard(
    segments: List<StatusSegment>,
    dotColor: Color,
    dotGlowing: Boolean,
    charge: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    CardShell(
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Box(
                modifier = Modifier.size(ArrowBoxSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(IconSize),
                )
            }
        },
    ) {
        Title()

        Spacer(Modifier.height(6.dp))

        StatusLine(segments = segments, dotColor = dotColor, dotGlowing = dotGlowing)

        Spacer(Modifier.height(8.dp))

        ChargeLine(charge = charge)
    }
}

@Composable
private fun BlockedCard(
    charge: Float,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    CardShell(
        onClick = null,
        modifier = modifier,
        trailing = {
            Badge(
                text = stringResource(R.string.ioni_badge_settings),
                color = colors.green,
                contentColor = colors.green,
                onClick = onSettingsClick,
            )
        },
    ) {
        Title()

        Spacer(Modifier.height(6.dp))

        StatusLine(
            segments = blockedSegments(),
            dotColor = colors.textSecondary,
            dotGlowing = false,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.ioni_status_blocked_hint),
            style = AuraTheme.typography.caption,
            color = colors.textDisabled,
        )

        Spacer(Modifier.height(8.dp))

        ChargeLine(charge = charge)
    }
}

@Composable
private fun CardShell(
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AuraCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        flat = true,
        glowOnPress = onClick != null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = RowMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(IconBoxSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    tint = AuraTheme.colors.accentBlue,
                    modifier = Modifier.size(IconSize),
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) { content() }

            trailing()
        }
    }
}

@Composable
private fun Title() {
    Text(
        text = stringResource(R.string.ioni_card_title),
        style = AuraTheme.typography.title,
        color = AuraTheme.colors.textBright,
    )
}

@Composable
private fun StatusLine(
    segments: List<StatusSegment>,
    dotColor: Color,
    dotGlowing: Boolean,
) {
    val colors = AuraTheme.colors
    val separator = stringResource(R.string.ioni_status_separator)

    Row(
        modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .then(
                    if (dotGlowing) Modifier.auraGlowLayers(colors.activeDotShadows) else Modifier
                )
                .clip(CircleShape)
                .background(dotColor)
        )
        segments.forEachIndexed { index, segment ->
            if (index > 0) SegmentText(text = separator, color = colors.textSecondary)
            SegmentText(text = segment.text, color = segment.color)
        }
    }
}

@Composable
private fun SegmentText(text: String, color: Color) {
    Text(
        text = text,
        style = AuraTheme.typography.caption,
        color = color,
        softWrap = false,
        overflow = TextOverflow.Visible,
    )
}

@Composable
private fun ChargeLine(
    charge: Float,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ChargeHeight)
            .clip(ChargeShape)
            .background(colors.border),
    ) {
        Box(
            Modifier
                .fillMaxWidth(charge.coerceIn(0f, 1f))
                .height(ChargeHeight)
                .clip(ChargeShape)
                .background(colors.accentBlue)
        )
    }
}

@Composable
private fun Badge(
    text: String,
    color: Color,
    contentColor: Color,
    onClick: (() -> Unit)? = null,
) {
    AuraPill(
        text = text,
        contentColor = contentColor,
        borderColor = color.copy(alpha = BADGE_BORDER_ALPHA),
        backgroundColor = color.copy(alpha = BADGE_FILL_ALPHA),
        horizontalPadding = 8.dp,
        topPadding = 5.dp,
        bottomPadding = 4.dp,
        borderWidth = 0.5.dp,
        textStyle = AuraTheme.typography.caption.copy(letterSpacing = BadgeLetterSpacing),
        onClick = onClick,
    )
}

@Composable
private fun idleSegments(): List<StatusSegment> {
    val muted = AuraTheme.colors.textSecondary
    return listOf(
        StatusSegment(stringResource(R.string.ioni_status_idle), muted),
        StatusSegment(stringResource(R.string.ioni_status_idle_charge), muted),
        StatusSegment(stringResource(R.string.ioni_status_idle_hint), muted),
    )
}

@Composable
private fun poweredSegments(hoursLeft: Int): List<StatusSegment> {
    val colors = AuraTheme.colors
    return listOf(
        StatusSegment(stringResource(R.string.ioni_status_powered), colors.green),
        StatusSegment(
            stringResource(R.string.ioni_status_hours_left, hoursLeft),
            colors.accentBlue,
        ),
        StatusSegment(stringResource(R.string.ioni_status_keep_alive), colors.textSecondary),
    )
}

@Composable
private fun blockedSegments(): List<StatusSegment> {
    val muted = AuraTheme.colors.textSecondary
    return listOf(
        StatusSegment(stringResource(R.string.ioni_status_powered), muted),
        StatusSegment(stringResource(R.string.ioni_status_blocked), muted),
    )
}

@Composable
private fun rememberChargeLeft(card: IoniCard): State<Duration> {
    val inspecting = LocalInspectionMode.current

    return produceState(card.chargeLeft(System.currentTimeMillis()), card, inspecting) {
        if (inspecting) return@produceState
        while (true) {
            value = card.chargeLeft(System.currentTimeMillis())
            delay(CHARGE_TICK)
        }
    }
}

private const val PREVIEW_CHARGED_FOR = 6L * 60 * 60 * 1000

@Preview(widthDp = 375, backgroundColor = 0xFF030507, showBackground = true)
@Composable
private fun IoniAiCardPreview() {
    val now = System.currentTimeMillis()

    AuraTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IoniAiCard(
                card = IoniCard(state = IoniState.COMING),
                isBatteryOptimizationDisabled = true,
                onOpenSheet = {},
                onSettingsClick = {},
            )
            IoniAiCard(
                card = IoniCard(state = IoniState.ACTIVE),
                isBatteryOptimizationDisabled = true,
                onOpenSheet = {},
                onSettingsClick = {},
            )
            IoniAiCard(
                card = IoniCard(
                    state = IoniState.ACTIVE,
                    lastCompletedTapAt = now - PREVIEW_CHARGED_FOR,
                ),
                isBatteryOptimizationDisabled = true,
                onOpenSheet = {},
                onSettingsClick = {},
            )
            IoniAiCard(
                card = IoniCard(
                    state = IoniState.ACTIVE,
                    lastCompletedTapAt = now - PREVIEW_CHARGED_FOR,
                ),
                isBatteryOptimizationDisabled = false,
                onOpenSheet = {},
                onSettingsClick = {},
            )
        }
    }
}
