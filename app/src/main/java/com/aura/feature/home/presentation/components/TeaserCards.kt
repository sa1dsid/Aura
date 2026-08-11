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
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.activeDotShadows
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.domain.model.Teasers
import com.aura.feature.home.presentation.format.StyledArg
import com.aura.feature.home.presentation.format.annotatedFormat
import com.aura.feature.home.presentation.format.displayName
import com.aura.feature.home.presentation.format.formatGrouped

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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TeaserCard(
            iconRes = R.drawable.ic_send_2,
            title = stringResource(R.string.teaser_bonus_title),
            subtitle = annotatedFormat(
                stringResource(R.string.teaser_bonus_steps),
                StyledArg(
                    teasers.bonusWithdrawal.completedSteps.toString(),
                    SpanStyle(color = colors.progressValue),
                ),
                StyledArg(
                    teasers.bonusWithdrawal.totalSteps.toString(),
                    SpanStyle(color = colors.progressTarget),
                ),
            ),
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
                    SpanStyle(color = colors.progressValue),
                ),
            ),
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
    enabled: Boolean = true,
    iconTint: Color? = null,
    leadingDotColor: Color? = null,
    progress: Float? = null,
) {
    val colors = AuraTheme.colors
    val contentAlpha = if (enabled) 1f else 0.4f

    AuraCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        flat = true,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = (iconTint ?: colors.textBright).copy(alpha = contentAlpha),
                    modifier = Modifier.size(22.dp),
                )

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AuraTheme.typography.title,
                        color = colors.textBright.copy(alpha = contentAlpha),
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (leadingDotColor != null) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .auraGlowLayers(colors.activeDotShadows)
                                    .clip(CircleShape)
                                    .background(leadingDotColor)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = subtitle,
                            style = AuraTheme.typography.caption,
                            color = colors.textSecondary.copy(alpha = contentAlpha),
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = contentAlpha),
                    modifier = Modifier.size(20.dp),
                )
            }

            if (progress != null) {
                TeaserProgressBar(progress = progress, enabled = enabled)
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
            .padding(horizontal = 14.dp)
            .padding(bottom = 12.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(colors.border),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    if (enabled) colors.textBright else colors.textTertiary
                )
        )
    }
}

