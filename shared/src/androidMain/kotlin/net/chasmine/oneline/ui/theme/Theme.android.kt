package net.chasmine.oneline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme as androidIsSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Android実装: システムのダークモード設定を取得
 */
@Composable
actual fun isSystemInDarkTheme(): Boolean = androidIsSystemInDarkTheme()
