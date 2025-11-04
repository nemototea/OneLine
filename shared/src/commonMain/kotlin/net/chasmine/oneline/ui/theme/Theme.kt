package net.chasmine.oneline.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// プラットフォーム固有のダークテーマ判定
expect fun isSystemInDarkTheme(): Boolean

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    primaryContainer = DarkPrimaryContainer,
    secondaryContainer = DarkSecondaryContainer,
    error = DarkError,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkSurface,
    onTertiary = DarkBackground,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onError = DarkBackground,
    outline = DarkOutline,
    onSurfaceVariant = DarkOnSurfaceVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    primaryContainer = LightPrimaryContainer,
    secondaryContainer = LightSecondaryContainer,
    error = LightError,
    onPrimary = LightOnPrimary,
    onSecondary = LightSurface,
    onTertiary = LightBackground,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onError = LightSurface,
    outline = LightOutline,
    onSurfaceVariant = LightOnSurfaceVariant,
)

@Composable
expect fun OneLineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
)

@Composable
fun BaseOneLineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: androidx.compose.material3.ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
