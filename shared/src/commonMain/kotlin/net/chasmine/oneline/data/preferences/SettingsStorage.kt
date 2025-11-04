package net.chasmine.oneline.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * 設定ストレージの共通インターフェース
 *
 * プラットフォーム固有の実装:
 * - Android: DataStore Preferences を使用
 * - iOS: UserDefaults を使用
 */
expect class SettingsStorage {
    /**
     * 文字列値を保存
     *
     * @param key 設定のキー
     * @param value 保存する値
     */
    suspend fun saveString(key: String, value: String)

    /**
     * 文字列値を取得
     *
     * @param key 設定のキー
     * @return 保存された値、存在しない場合はnull
     */
    suspend fun getString(key: String): String?

    /**
     * 真偽値を保存
     *
     * @param key 設定のキー
     * @param value 保存する値
     */
    suspend fun saveBoolean(key: String, value: Boolean)

    /**
     * 真偽値を取得
     *
     * @param key 設定のキー
     * @return 保存された値、存在しない場合はfalse
     */
    suspend fun getBoolean(key: String): Boolean

    /**
     * 文字列値の変更を監視
     *
     * @param key 設定のキー
     * @return 値の変更を通知するFlow
     */
    fun observeString(key: String): Flow<String?>

    /**
     * 真偽値の変更を監視
     *
     * @param key 設定のキー
     * @return 値の変更を通知するFlow
     */
    fun observeBoolean(key: String): Flow<Boolean>

    /**
     * 特定のキーの設定を削除
     *
     * @param key 削除する設定のキー
     */
    suspend fun remove(key: String)

    /**
     * すべての設定をクリア
     */
    suspend fun clear()
}
