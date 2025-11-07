package net.chasmine.oneline

import androidx.compose.ui.window.ComposeUIViewController
import net.chasmine.oneline.shared.App

/**
 * iOS用のメインViewController
 *
 * Compose MultiplatformのUIをiOSで表示するためのエントリーポイント
 */
fun MainViewController() = ComposeUIViewController {
    App()
}
