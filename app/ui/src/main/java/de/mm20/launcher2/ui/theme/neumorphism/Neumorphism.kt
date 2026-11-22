package de.mm20.launcher2.ui.theme.neumorphism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NeumorphismColors(
    val highlight: Color,
    val shadow: Color,
    val pressedHighlight: Color,
    val pressedShadow: Color,
)

val LocalNeumorphism = staticCompositionLocalOf {
    NeumorphismColors(
        highlight = Color.White.copy(alpha = 0.8f),
        shadow = Color.Black.copy(alpha = 0.35f),
        pressedHighlight = Color.White.copy(alpha = 0.45f),
        pressedShadow = Color.Black.copy(alpha = 0.25f),
    )
}

/**
 * Soft raised surface: a light wash on the top-left and a clean, blurred
 * shadow around the bottom-right. The shadow is delegated to Compose so it
 * stays soft on every device instead of ending in a hard painted rim.
 */
@Composable
fun Modifier.neumorphicSurface(
    shape: Shape,
    color: Color,
    pressed: Boolean = false,
    elevation: Dp = 10.dp,
): Modifier {
    val colors = LocalNeumorphism.current
    val shadow = if (pressed) colors.pressedShadow else colors.shadow
    val lift = if (pressed) elevation / 2 else elevation

    return this
        .shadow(
            elevation = lift,
            shape = shape,
            clip = false,
            ambientColor = shadow,
            spotColor = shadow,
        )
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    lerp(color, Color.White, 0.06f).copy(alpha = color.alpha),
                    color,
                ),
            ),
            shape = shape,
        )
        .clip(shape)
}

@Composable
fun NeuSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface,
    pressed: Boolean = false,
    elevation: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.neumorphicSurface(
            shape = shape,
            color = color,
            pressed = pressed,
            elevation = elevation,
        ),
        content = content,
    )
}
