package com.aura.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aura.core.designsystem.theme.AuraTheme
import kotlinx.coroutines.delay

private const val TOAST_HOLD_MILLIS = 2_500L

private const val TOAST_MOTION_MILLIS = 200

private val ToastShape = RoundedCornerShape(14.dp)

enum class AuraToastKind { ERROR, SUCCESS }

@Immutable
data class AuraToastMessage(
    val id: Long,
    val text: String,
    val kind: AuraToastKind,
)

@Stable
class AuraToastState {

    private var nextId = 0L

    var message by mutableStateOf<AuraToastMessage?>(null)
        private set

    var visible by mutableStateOf(false)
        private set

    fun show(text: String, kind: AuraToastKind) {
        message = AuraToastMessage(id = nextId++, text = text, kind = kind)
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

@Composable
fun rememberAuraToastState(): AuraToastState = remember { AuraToastState() }

@Composable
fun AuraToastHost(
    state: AuraToastState,
    modifier: Modifier = Modifier,
) {
    val message = state.message
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(message?.id, lifecycleOwner) {
        if (message == null) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(TOAST_HOLD_MILLIS)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = state.visible,
        enter = slideInVertically(tween(TOAST_MOTION_MILLIS)) { -it } +
            fadeIn(tween(TOAST_MOTION_MILLIS)),
        exit = fadeOut(tween(TOAST_MOTION_MILLIS)),
        modifier = modifier,
    ) {
        message?.let { AuraToastBanner(message = it, onClick = state::dismiss) }
    }
}

@Composable
private fun AuraToastBanner(
    message: AuraToastMessage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val accent = when (message.kind) {
        AuraToastKind.ERROR -> colors.danger
        AuraToastKind.SUCCESS -> colors.mint
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ToastShape)
            .background(Brush.verticalGradient(listOf(colors.surfaceTop, colors.surfaceBottom)))
            .border(1.dp, accent.copy(alpha = 0.42f), ToastShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToastGlyph(kind = message.kind, color = accent)

        Text(
            text = message.text,
            style = AuraTheme.typography.screenHint,
            color = colors.textBright,
        )
    }
}

@Composable
private fun ToastGlyph(
    kind: AuraToastKind,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f)),
    ) {
        val strokeWidth = 1.6.dp.toPx()

        when (kind) {
            AuraToastKind.ERROR -> {
                val inset = size.minDimension * 0.33f
                drawLine(
                    color = color,
                    start = Offset(inset, inset),
                    end = Offset(size.width - inset, size.height - inset),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width - inset, inset),
                    end = Offset(inset, size.height - inset),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            AuraToastKind.SUCCESS -> {
                val check = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.52f)
                    lineTo(size.width * 0.43f, size.height * 0.68f)
                    lineTo(size.width * 0.73f, size.height * 0.34f)
                }
                drawPath(
                    path = check,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Preview(widthDp = 375)
@Composable
private fun AuraToastErrorPreview() {
    AuraTheme {
        AuraToastBanner(
            message = AuraToastMessage(0, "Wrong email or password", AuraToastKind.ERROR),
            onClick = {},
            modifier = Modifier.padding(15.5.dp),
        )
    }
}

@Preview(widthDp = 375)
@Composable
private fun AuraToastSuccessPreview() {
    AuraTheme {
        AuraToastBanner(
            message = AuraToastMessage(0, "Reset link sent · check your email", AuraToastKind.SUCCESS),
            onClick = {},
            modifier = Modifier.padding(15.5.dp),
        )
    }
}
