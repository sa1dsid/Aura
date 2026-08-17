package com.aura.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme

private val TrackWidth = 41.dp

private val TrackHeight = 24.dp

private val HandleSize = 18.dp

private val HandleInset = 4.dp

private const val TRACK_FILL_ALPHA = 0.22f

private const val TRANSITION_MILLIS = 180

@Composable
fun AuraSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.green.copy(alpha = TRACK_FILL_ALPHA) else Color.Transparent,
        animationSpec = tween(TRANSITION_MILLIS),
        label = "switch-track",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) colors.green else colors.sheetBorder,
        animationSpec = tween(TRANSITION_MILLIS),
        label = "switch-border",
    )
    val handleColor by animateColorAsState(
        targetValue = if (checked) colors.green else colors.sheetMuted,
        animationSpec = tween(TRANSITION_MILLIS),
        label = "switch-handle",
    )
    val handleOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - HandleInset - HandleSize else HandleInset,
        animationSpec = tween(TRANSITION_MILLIS),
        label = "switch-handle-offset",
    )

    Box(
        modifier = modifier
            .width(TrackWidth)
            .height(TrackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, borderColor, CircleShape)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = handleOffset)
                .size(HandleSize)
                .clip(CircleShape)
                .background(handleColor)
        )
    }
}
