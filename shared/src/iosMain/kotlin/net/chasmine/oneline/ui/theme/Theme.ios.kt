package net.chasmine.oneline.ui.theme

import androidx.compose.runtime.Composable
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle

/**
 * iOS実装: システムのダークモード設定を取得
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean {
    val userInterfaceStyle = UITraitCollection.currentTraitCollection.userInterfaceStyle
    return userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
}
