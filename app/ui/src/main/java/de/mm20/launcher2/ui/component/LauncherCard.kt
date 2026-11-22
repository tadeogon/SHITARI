package de.mm20.launcher2.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.ui.theme.neumorphism.neumorphicSurface
import de.mm20.launcher2.ui.theme.transparency.transparency

@Composable
fun LauncherCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    backgroundOpacity: Float = MaterialTheme.transparency.surface,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface.copy(
        alpha = backgroundOpacity.coerceIn(
            0f,
            1f
        )
    ),
    content: @Composable () -> Unit = {}
) {
    val hasBackground = backgroundOpacity > 0.01f
    val surfaceModifier = if (hasBackground) {
        modifier.neumorphicSurface(
            shape = shape,
            color = color,
            elevation = elevation.coerceAtLeast(8.dp),
        )
    } else {
        modifier.clip(shape)
    }

    Box(modifier = surfaceModifier) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            content()
        }
    }
}
