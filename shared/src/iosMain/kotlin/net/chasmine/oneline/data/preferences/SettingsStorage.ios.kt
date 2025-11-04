package net.chasmine.oneline.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

/**
 * iOS向け設定ストレージ実装
 *
 * UserDefaults を使用してキー・バリュー形式でデータを保存します。
 */
actual class SettingsStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    // 値の変更を監視するためのMutableStateFlow
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String?>>()
    private val booleanFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()

    /**
     * 文字列値を保存
     */
    actual suspend fun saveString(key: String, value: String) {
        userDefaults.setObject(value, key)
        userDefaults.synchronize()

        // Flowを更新
        stringFlows[key]?.value = value
    }

    /**
     * 文字列値を取得
     */
    actual suspend fun getString(key: String): String? {
        return userDefaults.stringForKey(key)
    }

    /**
     * 真偽値を保存
     */
    actual suspend fun saveBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, key)
        userDefaults.synchronize()

        // Flowを更新
        booleanFlows[key]?.value = value
    }

    /**
     * 真偽値を取得
     */
    actual suspend fun getBoolean(key: String): Boolean {
        return userDefaults.boolForKey(key)
    }

    /**
     * 文字列値の変更を監視
     */
    actual fun observeString(key: String): Flow<String?> {
        // 既存のFlowがあればそれを返す
        return stringFlows.getOrPut(key) {
            // 現在の値で初期化
            val currentValue = userDefaults.stringForKey(key)
            MutableStateFlow(currentValue)
        }
    }

    /**
     * 真偽値の変更を監視
     */
    actual fun observeBoolean(key: String): Flow<Boolean> {
        // 既存のFlowがあればそれを返す
        return booleanFlows.getOrPut(key) {
            // 現在の値で初期化
            val currentValue = userDefaults.boolForKey(key)
            MutableStateFlow(currentValue)
        }
    }

    /**
     * 特定のキーの設定を削除
     */
    actual suspend fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()

        // Flowを更新
        stringFlows[key]?.value = null
        booleanFlows[key]?.value = false
    }

    /**
     * すべての設定をクリア
     */
    actual suspend fun clear() {
        // UserDefaultsの全データを削除
        val domain = userDefaults.dictionaryRepresentation().keys.toList()
        domain.forEach { key ->
            userDefaults.removeObjectForKey(key as String)
        }
        userDefaults.synchronize()

        // すべてのFlowをリセット
        stringFlows.values.forEach { it.value = null }
        booleanFlows.values.forEach { it.value = false }
    }
}
