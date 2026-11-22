package de.mm20.launcher2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import de.mm20.launcher2.preferences.ui.UiSettings
import de.mm20.launcher2.themes.ThemeRepository
import de.mm20.launcher2.ui.locals.LocalDarkTheme
import de.mm20.launcher2.ui.theme.colorscheme.darkColorSchemeOf
import de.mm20.launcher2.ui.theme.colorscheme.lightColorSchemeOf
import de.mm20.launcher2.ui.theme.neumorphism.LocalNeumorphism
import de.mm20.launcher2.ui.theme.neumorphism.NeumorphismColors
import de.mm20.launcher2.ui.theme.shapes.shapesOf
import de.mm20.launcher2.ui.theme.transparency.LocalTransparencyScheme
import de.mm20.launcher2.ui.theme.transparency.transparencySchemeOf
import de.mm20.launcher2.ui.theme.typography.typographyOf
import kotlinx.coroutines.flow.flatMapLatest
import org.koin.compose.koinInject
import de.mm20.launcher2.preferences.ColorScheme as ColorSchemePref


@Composable
fun LauncherTheme(
    content: @Composable () -> Unit
) {
    val uiSettings: UiSettings = koinInject()
    val themeRepository: ThemeRepository = koinInject()

    val themeColors by remember {
        uiSettings.colorsId.flatMapLatest {
            themeRepository.colors.getOrDefault(it)
        }
    }.collectAsState(null)

    val themeShapes by remember {
        uiSettings.shapesId.flatMapLatest {
            themeRepository.shapes.getOrDefault(it)
        }
    }.collectAsState(null)

    val themeTypography by remember {
        uiSettings.typographyId.flatMapLatest {
            themeRepository.typographies.getOrDefault(it)
        }
    }.collectAsState(null)

    val themeTransparencies by remember {
        uiSettings.transparenciesId.flatMapLatest {
            themeRepository.transparencies.getOrDefault(it)
        }
    }.collectAsState(null)

    val colorSchemePref by remember { uiSettings.colorScheme }.collectAsState(
        ColorSchemePref.System
    )
    val darkTheme =
        colorSchemePref == ColorSchemePref.Dark || colorSchemePref == ColorSchemePref.System && isSystemInDarkTheme()

    if (themeColors == null || themeShapes == null || themeTransparencies == null || themeTypography == null) {
        return
    }

    val colorScheme = if (darkTheme) {
        darkColorSchemeOf(themeColors!!)
    } else {
        lightColorSchemeOf(themeColors!!)
    }

    val shapes = shapesOf(themeShapes!!)
    val typography = typographyOf(themeTypography!!)

    val transparencyScheme = transparencySchemeOf(themeTransparencies!!)

    val neumorphismColors = remember(darkTheme) {
        if (darkTheme) {
            NeumorphismColors(
                highlight = Color.White.copy(alpha = 0.12f),
                shadow = Color.Black.copy(alpha = 0.48f),
                pressedHighlight = Color.White.copy(alpha = 0.06f),
                pressedShadow = Color.Black.copy(alpha = 0.35f),
            )
        } else {
            NeumorphismColors(
                highlight = Color.White.copy(alpha = 0.85f),
                shadow = Color(0xFFA0A0A0).copy(alpha = 0.35f),
                pressedHighlight = Color.White.copy(alpha = 0.55f),
                pressedShadow = Color(0xFFA0A0A0).copy(alpha = 0.28f),
            )
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalTransparencyScheme provides transparencyScheme,
        LocalNeumorphism provides neumorphismColors,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

