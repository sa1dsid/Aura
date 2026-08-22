package com.aura.feature.news.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.core.common.formatDayShort
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.component.brightDotShadows
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.news.domain.model.NewsItem

private val CardShape = RoundedCornerShape(16.dp)

private val CardPaddingHorizontal = 16.dp

private val CardPaddingVertical = 12.dp

private val HeaderGap = 8.dp

private val DateGap = 6.dp

private val DotSize = 6.dp

private val DotGap = 4.dp

private val ParagraphGap = 10.dp

private const val PREVIEW_LINES = 2

private const val EXPAND_MILLIS = 220

private const val EXPANDED_BORDER_ALPHA = 0.22f

@Composable
fun NewsCard(
    item: NewsItem,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by rememberPressedState(interactionSource)

    val borderColor by animateColorAsState(
        targetValue = when {
            expanded -> colors.accentBlueSoft.copy(alpha = EXPANDED_BORDER_ALPHA)
            pressed -> colors.borderStrong
            else -> colors.border
        },
        animationSpec = tween(EXPAND_MILLIS),
        label = "news-card-border",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressed = pressed)
            .clip(CardShape)
            .background(Brush.verticalGradient(listOf(colors.surfaceTop, colors.surfaceBottom)))
            .border(1.dp, borderColor, CardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .animateContentSize(tween(EXPAND_MILLIS))
            .padding(
                horizontal = CardPaddingHorizontal,
                vertical = CardPaddingVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(HeaderGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!item.read) {
                    Box(
                        Modifier
                            .size(DotSize)
                            .auraGlowLayers(colors.brightDotShadows)
                            .clip(CircleShape)
                            .background(colors.textBright)
                    )

                    Spacer(Modifier.width(DotGap))
                }

                Text(
                    text = item.title,
                    style = AuraTheme.typography.title,
                    color = if (item.read) colors.textSecondary else colors.textBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(DateGap))

            Text(
                text = item.publishedAt.formatDayShort(),
                style = AuraTheme.typography.badge,
                color = colors.textDisabled,
                maxLines = 1,
            )
        }

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(ParagraphGap)) {
                item.body.paragraphs().forEach { paragraph ->
                    Text(
                        text = paragraph,
                        style = AuraTheme.typography.body,
                        color = colors.textSecondary,
                    )
                }
            }
        } else {
            Text(
                text = item.body.preview(),
                style = AuraTheme.typography.body,
                color = colors.textSecondary,
                maxLines = PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun String.paragraphs(): List<String> =
    split('\n').map(String::trim).filter(String::isNotEmpty)

private fun String.preview(): String = paragraphs().joinToString(separator = " ")
