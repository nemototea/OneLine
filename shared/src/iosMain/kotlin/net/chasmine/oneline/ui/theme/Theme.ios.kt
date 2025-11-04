package net.chasmine.oneline.ui.theme

import androidx.compose.runtime.Composable
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

@Composable
actual fun isSystemInDarkTheme(): Boolean {
    return UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
}

@Composable
actual fun OneLineTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    // iOSではdynamicColorは未対応のため、通常のカラースキームを使用
    BaseOneLineTheme(
        darkTheme = darkTheme,
        content = content
    )
}
