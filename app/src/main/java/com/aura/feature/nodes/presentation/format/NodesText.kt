package com.aura.feature.nodes.presentation.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Dp
import com.aura.core.designsystem.component.auraBlurRadius

const val NODES_ITALIC_SKEW = -0.208f

val NodesItalic = TextGeometricTransform(skewX = NODES_ITALIC_SKEW)

@Composable
fun textGlow(color: Color, blur: Dp): Shadow {
    val density = LocalDensity.current
    val radius = remember(density, blur) { with(density) { auraBlurRadius(blur).toPx() } }
    return Shadow(color = color, blurRadius = radius)
}
