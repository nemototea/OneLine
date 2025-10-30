package net.chasmine.oneline.shared

/**
 * プラットフォーム情報を使った挨拶メッセージを生成するクラス
 * KMP/CMP の動作確認用
 */
class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello from ${platform.name}!"
    }
}
