package com.aura.feature.home.presentation.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraBottomSheet
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.AuraSheetCloseButton
import com.aura.core.designsystem.component.activeDotShadows
import com.aura.core.designsystem.component.auraArcGlow
import com.aura.core.designsystem.component.auraBlurRadius
import com.aura.core.designsystem.component.auraDropShadows
import com.aura.core.designsystem.component.drawAuraArcGlow
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraColors
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.SPARK_COUPON_THRESHOLD
import com.aura.feature.home.domain.model.SparkTeaser
import com.aura.feature.home.presentation.format.StyledArg
import com.aura.feature.home.presentation.format.annotatedFormat
import com.aura.feature.home.presentation.format.formatGrouped

private val HeaderGap = 8.dp

private val HeadingSlot = 15.dp

private val HintSlot = 26.dp

private val GaugeSize = 185.dp

private val GaugeTrackStroke = 13.5.dp

private val GaugeArcStroke = 8.dp

private val GaugeTrackInset = 6.75.dp

private val GaugeArcInset = 6.5.dp

private val TicketShape = RoundedCornerShape(16.dp)

private val TicketCorner = 16.dp

private val PillShape = RoundedCornerShape(percent = 50)

private val CopyIconSize = 12.dp

private val ActionHeight = 44.dp

private val ActionIconSize = 16.dp

private val CodeGlowBlur = 6.dp

private const val GAUGE_START_ANGLE = 140f

private const val GAUGE_SWEEP_ANGLE = 216f

private const val TICKET_FILL_ALPHA = 0.08f

private const val TICKET_BORDER_WIDTH = 0.5f

private const val PILL_FILL_ALPHA = 0.22f

private const val ACTION_BORDER_ALPHA = 0.60f

private const val CODE_GLOW_ALPHA = 0.80f

@Composable
fun SparkSheet(
    spark: SparkTeaser?,
    onDismissRequest: () -> Unit,
    onCopyCodeClick: (String) -> Unit,
    onOpenSigmaDropClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(spark) }
    if (spark != null && spark != shown) shown = spark

    AuraBottomSheet(
        visible = spark != null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        val state = shown ?: return@AuraBottomSheet
        val code = state.readyCode

        if (code == null) {
            ProgressBody(spark = state, onDismissRequest = onDismissRequest)
        } else {
            CodeBody(
                code = code,
                target = state.target,
                onDismissRequest = onDismissRequest,
                onCopyCodeClick = { onCopyCodeClick(code) },
                onOpenSigmaDropClick = onOpenSigmaDropClick,
            )
        }
    }
}

@Composable
private fun ColumnScope.ProgressBody(
    spark: SparkTeaser,
    onDismissRequest: () -> Unit,
) {
    Header(
        subtitle = stringResource(R.string.spark_sheet_subtitle),
        onDismissRequest = onDismissRequest,
    )

    Spacer(Modifier.height(32.dp))

    SparkGauge(spark = spark)

    Spacer(Modifier.height(32.dp))

    Text(
        text = stringResource(R.string.spark_sheet_note),
        style = AuraTheme.typography.cardCaption,
        color = AuraTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ColumnScope.CodeBody(
    code: String,
    target: Long,
    onDismissRequest: () -> Unit,
    onCopyCodeClick: () -> Unit,
    onOpenSigmaDropClick: () -> Unit,
) {
    val colors = AuraTheme.colors
    val amount = target.formatGrouped()

    Header(
        subtitle = stringResource(R.string.spark_sheet_ready_subtitle, amount),
        onDismissRequest = onDismissRequest,
    )

    Spacer(Modifier.height(24.dp))

    CodeTicket(code = code, onCopyCodeClick = onCopyCodeClick)

    Spacer(Modifier.height(16.dp))

    Text(
        text = annotatedFormat(
            stringResource(R.string.spark_sheet_hint),
            StyledArg(
                text = stringResource(R.string.spark_sheet_hint_app),
                style = SpanStyle(color = colors.green),
            ),
            StyledArg(
                text = stringResource(R.string.spark_sheet_hint_amount, amount),
                style = SpanStyle(color = colors.green),
            ),
        ),
        style = AuraTheme.typography.title,
        color = colors.textBright,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .textSlot(HintSlot),
    )

    Spacer(Modifier.height(24.dp))

    OpenAppAction(
        text = stringResource(R.string.spark_sheet_open),
        onClick = onOpenSigmaDropClick,
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.spark_sheet_reset_note),
        style = AuraTheme.typography.cardCaption,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Header(
    subtitle: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HeaderGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.spark_sheet_title),
                style = AuraTheme.typography.sheetHeading,
                color = colors.textBright,
                modifier = Modifier.textSlot(HeadingSlot),
            )

            Spacer(Modifier.height(HeaderGap))

            Text(
                text = subtitle,
                style = AuraTheme.typography.cardCaption,
                color = colors.textSecondary,
            )
        }

        AuraSheetCloseButton(onClick = onDismissRequest)
    }
}

@Composable
private fun ColumnScope.SparkGauge(spark: SparkTeaser) {
    val colors = AuraTheme.colors
    val progress = rememberUpdatedState(spark.progress)

    val gaugeModifier = remember(colors) {
        Modifier.gaugeGraphics(
            progress = { progress.value },
            trackColor = colors.borderMuted,
            edgeColor = colors.sparkArcEdge,
            arcColor = colors.accentBlue,
            arcShadows = colors.sparkArcShadows,
        )
    }

    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(GaugeSize)
            .then(gaugeModifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = spark.collected.formatGrouped(),
                style = AuraTheme.typography.speedValue,
                color = colors.textBright,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.spark_sheet_target, spark.target.formatGrouped()),
                style = AuraTheme.typography.listRowTitle,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = stringResource(R.string.spark_sheet_percent, spark.percentToCode),
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun Modifier.gaugeGraphics(
    progress: () -> Float,
    trackColor: Color,
    edgeColor: Color,
    arcColor: Color,
    arcShadows: List<AuraShadow>,
): Modifier = drawWithCache {
    val trackInset = GaugeTrackInset.toPx()
    val trackTopLeft = Offset(trackInset, trackInset)
    val trackSize = Size(size.width - trackInset * 2f, size.height - trackInset * 2f)

    val arcInset = GaugeArcInset.toPx()
    val arcTopLeft = Offset(arcInset, arcInset)
    val arcSize = Size(size.width - arcInset * 2f, size.height - arcInset * 2f)

    val trackStroke = GaugeTrackStroke.toPx()
    val arcStroke = GaugeArcStroke.toPx()
    val glows = arcShadows.map { auraArcGlow(it, arcStroke) }

    onDrawBehind {
        drawArc(
            color = trackColor,
            startAngle = GAUGE_START_ANGLE,
            sweepAngle = GAUGE_SWEEP_ANGLE,
            useCenter = false,
            topLeft = trackTopLeft,
            size = trackSize,
            style = Stroke(width = trackStroke, cap = StrokeCap.Round),
        )

        val sweep = GAUGE_SWEEP_ANGLE * progress()
        if (sweep <= 0f) return@onDrawBehind

        glows.forEach { glow ->
            drawAuraArcGlow(glow, arcTopLeft, arcSize, GAUGE_START_ANGLE, sweep)
        }

        drawArc(
            color = edgeColor,
            startAngle = GAUGE_START_ANGLE,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = trackTopLeft,
            size = trackSize,
            style = Stroke(width = trackStroke, cap = StrokeCap.Round),
        )

        drawArc(
            color = arcColor,
            startAngle = GAUGE_START_ANGLE,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = arcStroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ColumnScope.CodeTicket(
    code: String,
    onCopyCodeClick: () -> Unit,
) {
    val colors = AuraTheme.colors
    val density = LocalDensity.current
    val codeGlow = remember(density) { with(density) { auraBlurRadius(CodeGlowBlur).toPx() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .auraDropShadows(
                shadows = colors.activeDotShadows,
                cornerRadius = TicketCorner,
                outsideOnly = true,
            )
            .clip(TicketShape)
            .background(colors.green.copy(alpha = TICKET_FILL_ALPHA))
            .border(TICKET_BORDER_WIDTH.dp, colors.green, TicketShape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.spark_sheet_code_label),
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = code,
            style = AuraTheme.typography.metricNumber.copy(
                shadow = Shadow(
                    color = colors.green.copy(alpha = CODE_GLOW_ALPHA),
                    blurRadius = codeGlow,
                ),
            ),
            color = colors.green,
        )

        Spacer(Modifier.height(16.dp))

        CopyAction(onClick = onCopyCodeClick)
    }
}

@Composable
private fun CopyAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    Row(
        modifier = modifier
            .pressScale(pressed = isPressed)
            .clip(PillShape)
            .background(colors.iceBlue.copy(alpha = PILL_FILL_ALPHA))
            .border(TICKET_BORDER_WIDTH.dp, colors.textIce, PillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(start = 12.dp, end = 12.dp, top = 7.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_copy),
            contentDescription = null,
            modifier = Modifier.size(CopyIconSize),
        )

        Text(
            text = stringResource(R.string.spark_sheet_copy),
            style = AuraTheme.typography.title,
            color = colors.textBright,
        )
    }
}

@Composable
private fun ColumnScope.OpenAppAction(
    text: String,
    onClick: () -> Unit,
) {
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
            text = text,
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

private fun Modifier.textSlot(height: Dp): Modifier =
    height(height).wrapContentHeight(unbounded = true)

private val AuraColors.sparkArcShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(glowSky.copy(alpha = 0.10f), 12.8.dp),
        AuraShadow(glowSky.copy(alpha = 0.40f), 51.dp, 0.9.dp),
        AuraShadow(glowSky.copy(alpha = 0.60f), 40.8.dp, 2.8.dp),
    )

@Preview(widthDp = 375, heightDp = 420, backgroundColor = 0xFF030507, showBackground = true)
@Composable
private fun SparkSheetProgressPreview() {
    AuraTheme {
        SparkSheet(
            spark = SparkTeaser(collected = 142_800, target = SPARK_COUPON_THRESHOLD),
            onDismissRequest = {},
            onCopyCodeClick = {},
            onOpenSigmaDropClick = {},
        )
    }
}

@Preview(widthDp = 375, heightDp = 440, backgroundColor = 0xFF030507, showBackground = true)
@Composable
private fun SparkSheetCodePreview() {
    AuraTheme {
        SparkSheet(
            spark = SparkTeaser(
                collected = 0,
                target = SPARK_COUPON_THRESHOLD,
                issuedCoupons = 1,
                couponLimit = 4,
                readyCode = "A8X4-KP92-QW01",
            ),
            onDismissRequest = {},
            onCopyCodeClick = {},
            onOpenSigmaDropClick = {},
        )
    }
}
