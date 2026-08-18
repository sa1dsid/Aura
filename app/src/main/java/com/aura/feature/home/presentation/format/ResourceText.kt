package com.aura.feature.home.presentation.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.aura.R
import com.aura.feature.home.domain.model.NodeTier

private val PLACEHOLDER_PATTERN = Regex("""%(\d+)\$[sd](%%)?""")

data class StyledArg(
    val text: String,
    val style: SpanStyle? = null,
)

fun annotatedFormat(
    template: String,
    vararg args: StyledArg,
    gapStyle: SpanStyle? = null,
): AnnotatedString = buildAnnotatedString {
    var consumedUpTo = 0
    var afterPlaceholder = false
    PLACEHOLDER_PATTERN.findAll(template).forEach { match ->
        appendLiteral(
            literal = template.substring(consumedUpTo, match.range.first),
            gapStyle = gapStyle,
            gapBefore = afterPlaceholder,
            gapAfter = true,
        )
        consumedUpTo = match.range.last + 1
        afterPlaceholder = true

        val arg = args[match.groupValues[1].toInt() - 1]
        val text = if (match.groupValues[2].isEmpty()) arg.text else arg.text + "%"
        val style = arg.style
        if (style == null) append(text) else withStyle(style) { append(text) }
    }
    appendLiteral(
        literal = template.substring(consumedUpTo),
        gapStyle = gapStyle,
        gapBefore = afterPlaceholder,
        gapAfter = false,
    )
}

private fun AnnotatedString.Builder.appendLiteral(
    literal: String,
    gapStyle: SpanStyle?,
    gapBefore: Boolean,
    gapAfter: Boolean,
) {
    val text = literal.replace("%%", "%")
    if (gapStyle == null) {
        append(text)
        return
    }

    val bodyStart = text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: text.length
    val bodyEnd = text.indexOfLast { !it.isWhitespace() } + 1
    val gapStart = if (gapBefore) bodyStart else 0
    val gapEnd = if (gapAfter) maxOf(bodyEnd, gapStart) else text.length

    if (gapStart > 0) withStyle(gapStyle) { append(text.substring(0, gapStart)) }
    append(text.substring(gapStart, gapEnd))
    if (gapEnd < text.length) withStyle(gapStyle) { append(text.substring(gapEnd)) }
}

@Composable
fun NodeTier.displayName(): String = stringResource(labelRes())

@Composable
fun NodeTier.stepLabel(): String = displayName().replace(' ', '\n')

@StringRes
private fun NodeTier.labelRes(): Int = when (this) {
    NodeTier.IDLE_NODE -> R.string.tier_idle
    NodeTier.ACTIVE_SIGNAL -> R.string.tier_active_signal
    NodeTier.STABLE_LINK -> R.string.tier_stable_link
    NodeTier.CORE_NODE -> R.string.tier_core_node
    NodeTier.IONIC_PRIME -> R.string.tier_ionic_prime
}

