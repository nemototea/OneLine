package net.chasmine.oneline.shared

import platform.UIKit.UIDevice

/**
 * iOS プラットフォーム実装
 */
class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * iOS プラットフォーム情報を取得する actual 実装
 */
actual fun getPlatform(): Platform = IOSPlatform()
