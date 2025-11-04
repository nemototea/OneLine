package net.chasmine.oneline.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun isSystemInDarkTheme(): Boolean {
    return androidx.compose.foundation.isSystemInDarkTheme()
}

@Composable
actual fun OneLineTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> androidx.compose.material3.darkColorScheme(
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
        else -> androidx.compose.material3.lightColorScheme(
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
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    BaseOneLineTheme(
        darkTheme = darkTheme,
        colorScheme = colorScheme,
        content = content
    )
}
