package com.aura.feature.home.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraPill
import com.aura.core.designsystem.component.activeDotShadows
import com.aura.core.designsystem.component.auraDropShadows
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.Teasers
import com.aura.feature.home.presentation.format.StyledArg
import com.aura.feature.home.presentation.format.annotatedFormat
import com.aura.feature.home.presentation.format.displayName
import com.aura.feature.home.presentation.format.formatGrouped

private val IconBoxSize = 32.dp

private val IconSize = 24.dp

private val ArrowBoxSize = 48.dp

private val ProgressHeight = 1.dp

private val CardCorner = 16.dp

private val BadgeSidePadding = 8.dp

private val BadgeTopPadding = 5.dp

private val BadgeBottomPadding = 4.dp

private val BadgeBorder = 0.5.dp

private const val BADGE_FILL_ALPHA = 0.22f

private const val BADGE_BORDER_ALPHA = 0.55f

val CardGap = 12.dp

private const val MONO_SPACE_EM = 0.6f

private const val COUNTER_GAP_EM = 0.28f

private val CounterGap = SpanStyle(letterSpacing = (COUNTER_GAP_EM - MONO_SPACE_EM).em)

@Composable
fun TeaserCards(
    teasers: Teasers,
    currentTier: NodeTier,
    onBonusWithdrawalClick: () -> Unit,
    onSparkClick: () -> Unit,
    onVpnCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val typography = AuraTheme.typography
    val counterSize = typography.badge.fontSize
    val counterLine = typography.caption.copy(lineHeight = typography.badge.lineHeight)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        TeaserCard(
            iconRes = R.drawable.ic_send_2,
            title = stringResource(R.string.teaser_bonus_title),
            subtitle = annotatedFormat(
                stringResource(R.string.teaser_bonus_steps),
                StyledArg(
                    teasers.bonusWithdrawal.completedSteps.toString(),
                    SpanStyle(color = colors.progressValue, fontSize = counterSize),
                ),
                StyledArg(
                    teasers.bonusWithdrawal.totalSteps.toString(),
                    SpanStyle(color = colors.progressTarget, fontSize = counterSize),
                ),
                gapStyle = CounterGap,
            ),
            subtitleStyle = counterLine,
            badgeText = if (teasers.bonusWithdrawal.isComplete) {
                stringResource(R.string.teaser_complete)
            } else {
                null
            },
            blinking = teasers.bonusWithdrawal.isBlinking,
            onClick = onBonusWithdrawalClick,
        )

        TeaserCard(
            iconRes = R.drawable.ic_flash,
            title = stringResource(R.string.teaser_spark_title),
            subtitle = annotatedFormat(
                stringResource(R.string.teaser_spark_progress),
                StyledArg(
                    teasers.spark.collected.formatGrouped(),
                    SpanStyle(color = colors.progressValue),
                ),
                StyledArg(
                    teasers.spark.target.formatGrouped(),
                    SpanStyle(color = colors.progressTarget),
                ),
                gapStyle = CounterGap,
            ),
            onClick = onSparkClick,
        )

        TeaserCard(
            iconRes = R.drawable.ic_shield,
            title = stringResource(R.string.teaser_vpn_title),
            subtitle = annotatedFormat(
                stringResource(R.string.teaser_vpn_sub),
                StyledArg(currentTier.displayName()),
                StyledArg(
                    teasers.vpnCode.contributionPercent.toString(),
                    SpanStyle(color = colors.progressValue, fontSize = counterSize),
                ),
            ),
            subtitleStyle = counterLine,
            enabled = teasers.vpnCode.isEnabled,
            iconTint = colors.accentBlue,
            leadingDotColor = if (teasers.vpnCode.isEnabled) colors.green else null,
            progress = teasers.vpnCode.contributionPercent / 100f,
            onClick = onVpnCodeClick,
        )
    }
}

@Composable
private fun TeaserCard(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: AnnotatedString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleStyle: TextStyle = AuraTheme.typography.caption,
    enabled: Boolean = true,
    iconTint: Color? = null,
    leadingDotColor: Color? = null,
    progress: Float? = null,
    badgeText: String? = null,
    blinking: Boolean = false,
) {
    val colors = AuraTheme.colors
    val contentAlpha = if (enabled) 1f else 0.4f
    val isComplete = badgeText != null

    AuraCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isComplete) {
                    Modifier.auraDropShadows(colors.activeDotShadows, CardCorner)
                } else {
                    Modifier
                }
            ),
        onClick = onClick,
        enabled = enabled,
        flat = true,
        glowOnPress = true,
        blinking = blinking,
        accentBorderColor = if (isComplete) colors.green else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(IconBoxSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = (iconTint ?: colors.textBright).copy(alpha = contentAlpha),
                    modifier = Modifier.size(IconSize),
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AuraTheme.typography.title,
                    color = colors.textBright.copy(alpha = contentAlpha),
                )

                Spacer(Modifier.height(6.dp))

                if (badgeText != null) {
                    AuraPill(
                        text = badgeText,
                        contentColor = colors.green,
                        borderColor = colors.green.copy(alpha = BADGE_BORDER_ALPHA),
                        backgroundColor = colors.green.copy(alpha = BADGE_FILL_ALPHA),
                        horizontalPadding = BadgeSidePadding,
                        topPadding = BadgeTopPadding,
                        bottomPadding = BadgeBottomPadding,
                        borderWidth = BadgeBorder,
                        textStyle = AuraTheme.typography.listValue,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (leadingDotColor != null) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .auraGlowLayers(colors.activeDotShadows)
                                    .clip(CircleShape)
                                    .background(leadingDotColor)
                            )
                        }
                        Text(
                            text = subtitle,
                            style = subtitleStyle,
                            color = colors.textSecondary.copy(alpha = contentAlpha),
                        )
                    }
                }

                if (progress != null) {
                    Spacer(Modifier.height(8.dp))
                    TeaserProgressBar(progress = progress, enabled = enabled)
                }
            }

            Box(
                modifier = Modifier.size(ArrowBoxSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = contentAlpha),
                    modifier = Modifier.size(IconSize),
                )
            }
        }
    }
}

@Composable
private fun TeaserProgressBar(
    progress: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ProgressHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(colors.border),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(ProgressHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    if (enabled) colors.textBright else colors.textTertiary
                )
        )
    }
}
