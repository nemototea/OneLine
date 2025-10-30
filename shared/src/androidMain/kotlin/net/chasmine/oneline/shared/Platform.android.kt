package net.chasmine.oneline.shared

/**
 * Android プラットフォーム実装
 */
class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

/**
 * Android プラットフォーム情報を取得する actual 実装
 */
actual fun getPlatform(): Platform = AndroidPlatform()
