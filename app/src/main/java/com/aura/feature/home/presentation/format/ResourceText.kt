package com.aura.feature.home.presentation.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.aura.R
import com.aura.feature.home.domain.model.NetworkType
import com.aura.feature.home.domain.model.NodeTier

private val PLACEHOLDER_PATTERN = Regex("""%(\d+)\$[sd]""")

data class StyledArg(
    val text: String,
    val style: SpanStyle? = null,
)

fun annotatedFormat(template: String, vararg args: StyledArg): AnnotatedString =
    buildAnnotatedString {
        var consumedUpTo = 0
        PLACEHOLDER_PATTERN.findAll(template).forEach { match ->
            append(template.substring(consumedUpTo, match.range.first).replace("%%", "%"))
            consumedUpTo = match.range.last + 1

            val arg = args[match.groupValues[1].toInt() - 1]
            val style = arg.style
            if (style == null) append(arg.text) else withStyle(style) { append(arg.text) }
        }
        append(template.substring(consumedUpTo).replace("%%", "%"))
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

@Composable
fun NetworkType.label(): String = stringResource(
    when (this) {
        NetworkType.MOBILE_2G -> R.string.network_mobile_2g
        NetworkType.MOBILE_3G -> R.string.network_mobile_3g
        NetworkType.MOBILE_4G -> R.string.network_mobile_4g
        NetworkType.MOBILE_5G -> R.string.network_mobile_5g
        NetworkType.WIFI -> R.string.network_wifi
        NetworkType.NONE -> R.string.network_none
    }
)
