package net.chasmine.oneline.shared

/**
 * プラットフォーム情報を提供するインターフェース
 * expect/actual パターンで各プラットフォーム固有の実装を提供する
 */
interface Platform {
    val name: String
}

/**
 * 現在のプラットフォーム情報を取得する
 */
expect fun getPlatform(): Platform
