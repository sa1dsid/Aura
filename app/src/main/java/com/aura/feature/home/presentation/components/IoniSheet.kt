package com.aura.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraBottomSheet
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme

enum class IoniSheetKind { COMING, GEO, IDLE, LIVE }

private val SheetHorizontalPadding = 36.dp

private val SheetBottomGap = 20.dp

private val SheetTopPadding = 13.dp

private val GrabberWidth = 34.dp

private val OrbWidth = 76.dp

private val OrbHeight = 82.8.dp

private val OrbShape = RoundedCornerShape(38.dp)

private val OrbBorderWidth = 2.dp

private val OrbTopInset = 6.8.dp

private val OrbGlowBlur = 24.dp

private val SparkSize = 20.dp

private val ActionHeight = 46.dp

private val ActionShape = RoundedCornerShape(percent = 50)

private val ActionGlowBlur = 16.dp

private val ActionCorner = 23.dp

private const val ORB_GLOW_ALPHA = 0.40f

private const val ORB_INNER_ALPHA = 0.08f

private const val ORB_INNER_START = 0.55f

private const val ORB_FILL_ALPHA = 0.002f

private const val ACTION_GLOW_ALPHA = 0.28f

@Composable
fun IoniSheet(
    kind: IoniSheetKind?,
    isAppInstalled: Boolean,
    onDismissRequest: () -> Unit,
    onOpenIoniClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(kind) }
    if (kind != null && kind != shown) shown = kind

    AuraBottomSheet(
        visible = kind != null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        horizontalPadding = SheetHorizontalPadding,
        bottomGap = SheetBottomGap,
        topPadding = SheetTopPadding,
        grabberWidth = GrabberWidth,
        grabberColor = AuraTheme.colors.sheetGrabberSoft,
    ) {
        when (shown) {
            IoniSheetKind.COMING -> AnnouncementBody(
                title = stringResource(R.string.ioni_sheet_coming_title),
                text = stringResource(R.string.ioni_sheet_coming_text),
                onDismissRequest = onDismissRequest,
            )

            IoniSheetKind.GEO -> AnnouncementBody(
                title = stringResource(R.string.ioni_sheet_geo_title),
                text = stringResource(R.string.ioni_sheet_geo_text),
                onDismissRequest = onDismissRequest,
            )

            IoniSheetKind.IDLE -> IdleBody(onDismissRequest = onDismissRequest)

            IoniSheetKind.LIVE -> LiveBody(
                isAppInstalled = isAppInstalled,
                onDismissRequest = onDismissRequest,
                onOpenIoniClick = onOpenIoniClick,
            )

            null -> Unit
        }
    }
}

@Composable
private fun ColumnScope.AnnouncementBody(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
) {
    SheetBody(
        title = title,
        text = text,
        orbBorderColor = AuraTheme.colors.textSecondary,
        sparkColor = AuraTheme.colors.textBright,
        orbGlowing = true,
    )

    Spacer(Modifier.height(10.dp))

    PrimaryAction(
        text = stringResource(R.string.ioni_sheet_got_it),
        onClick = onDismissRequest,
    )
}

@Composable
private fun ColumnScope.IdleBody(onDismissRequest: () -> Unit) {
    SheetBody(
        title = stringResource(R.string.ioni_sheet_idle_title),
        text = stringResource(R.string.ioni_sheet_idle_text),
        orbBorderColor = AuraTheme.colors.textDisabled,
        sparkColor = AuraTheme.colors.textDisabled,
        orbGlowing = false,
    )

    Spacer(Modifier.height(10.dp))

    PrimaryAction(
        text = stringResource(R.string.ioni_sheet_got_it),
        onClick = onDismissRequest,
    )
}

@Composable
private fun ColumnScope.LiveBody(
    isAppInstalled: Boolean,
    onDismissRequest: () -> Unit,
    onOpenIoniClick: () -> Unit,
) {
    SheetBody(
        title = stringResource(R.string.ioni_sheet_live_title),
        text = stringResource(R.string.ioni_sheet_live_text),
        orbBorderColor = AuraTheme.colors.textSecondary,
        sparkColor = AuraTheme.colors.textBright,
        orbGlowing = true,
    )

    Spacer(Modifier.height(10.dp))

    PrimaryAction(
        text = stringResource(
            if (isAppInstalled) R.string.ioni_sheet_open else R.string.ioni_sheet_install
        ),
        onClick = onOpenIoniClick,
    )

    Spacer(Modifier.height(16.dp))

    LaterAction(onClick = onDismissRequest)
}

@Composable
private fun ColumnScope.SheetBody(
    title: String,
    text: String,
    orbBorderColor: Color,
    sparkColor: Color,
    orbGlowing: Boolean,
) {
    Spacer(Modifier.height(2.dp))

    Orb(borderColor = orbBorderColor, sparkColor = sparkColor, glowing = orbGlowing)

    Spacer(Modifier.height(16.8.dp))

    Text(
        text = title,
        style = AuraTheme.typography.sheetHeading,
        color = AuraTheme.colors.textBright,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = text,
        style = AuraTheme.typography.title,
        color = AuraTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(8.8.dp))
}

@Composable
private fun Orb(
    borderColor: Color,
    sparkColor: Color,
    glowing: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val innerGlow = colors.bonusBadgeGlow.copy(alpha = ORB_INNER_ALPHA)

    Box(
        modifier = modifier
            .size(width = OrbWidth, height = OrbHeight)
            .then(
                if (glowing) {
                    Modifier.auraGlow(
                        color = colors.bonusBadgeGlow.copy(alpha = ORB_GLOW_ALPHA),
                        width = OrbWidth,
                        height = OrbHeight,
                        blurRadius = OrbGlowBlur,
                    )
                } else {
                    Modifier
                }
            )
            .clip(OrbShape)
            .background(Color.White.copy(alpha = ORB_FILL_ALPHA))
            .then(if (glowing) Modifier.innerGlow(innerGlow) else Modifier)
            .border(OrbBorderWidth, borderColor, OrbShape)
            .padding(top = OrbTopInset),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_spark),
            contentDescription = null,
            tint = sparkColor,
            modifier = Modifier.size(SparkSize),
        )
    }
}

private fun Modifier.innerGlow(color: Color): Modifier = drawWithCache {
    val brush = Brush.radialGradient(
        colorStops = arrayOf(
            ORB_INNER_START to Color.Transparent,
            1f to color,
        ),
        center = Offset(size.width / 2f, size.height / 2f),
        radius = size.maxDimension / 2f,
    )

    onDrawWithContent {
        drawContent()
        drawRect(brush = brush)
    }
}

@Composable
private fun ColumnScope.PrimaryAction(
    text: String,
    onClick: () -> Unit,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionHeight)
            .pressScale(pressed = isPressed)
            .auraDropShadow(
                color = colors.sheetActionBorder.copy(alpha = ACTION_GLOW_ALPHA),
                blurRadius = ActionGlowBlur,
                cornerRadius = ActionCorner,
            )
            .clip(ActionShape)
            .background(colors.sheetActionBackground)
            .border(1.dp, colors.sheetActionBorder, ActionShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.secondaryButtonLabel.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ColumnScope.LaterAction(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Text(
        text = stringResource(R.string.ioni_sheet_later),
        style = AuraTheme.typography.title,
        color = AuraTheme.colors.textDisabled,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Preview(widthDp = 375, heightDp = 420, backgroundColor = 0xFF030507, showBackground = true)
@Composable
private fun IoniSheetLivePreview() {
    AuraTheme {
        IoniSheet(
            kind = IoniSheetKind.LIVE,
            isAppInstalled = false,
            onDismissRequest = {},
            onOpenIoniClick = {},
        )
    }
}

@Preview(widthDp = 375, heightDp = 400, backgroundColor = 0xFF030507, showBackground = true)
@Composable
private fun IoniSheetIdlePreview() {
    AuraTheme {
        IoniSheet(
            kind = IoniSheetKind.IDLE,
            isAppInstalled = false,
            onDismissRequest = {},
            onOpenIoniClick = {},
        )
    }
}
