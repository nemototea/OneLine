package net.chasmine.oneline.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStoreのシングルトンインスタンスを作成
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Android向け設定ストレージ実装
 *
 * DataStore Preferences を使用してキー・バリュー形式でデータを保存します。
 */
actual class SettingsStorage(private val context: Context) {

    /**
     * 文字列値を保存
     */
    actual suspend fun saveString(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    /**
     * 文字列値を取得
     */
    actual suspend fun getString(key: String): String? {
        val prefKey = stringPreferencesKey(key)
        val preferences = context.dataStore.data.first()
        return preferences[prefKey]
    }

    /**
     * 真偽値を保存
     */
    actual suspend fun saveBoolean(key: String, value: Boolean) {
        val prefKey = booleanPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }

    /**
     * 真偽値を取得
     */
    actual suspend fun getBoolean(key: String): Boolean {
        val prefKey = booleanPreferencesKey(key)
        val preferences = context.dataStore.data.first()
        return preferences[prefKey] ?: false
    }

    /**
     * 文字列値の変更を監視
     */
    actual fun observeString(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[prefKey]
        }
    }

    /**
     * 真偽値の変更を監視
     */
    actual fun observeBoolean(key: String): Flow<Boolean> {
        val prefKey = booleanPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[prefKey] ?: false
        }
    }

    /**
     * 特定のキーの設定を削除
     */
    actual suspend fun remove(key: String) {
        // String型とBoolean型の両方を試して削除
        context.dataStore.edit { preferences ->
            val stringKey = stringPreferencesKey(key)
            val booleanKey = booleanPreferencesKey(key)
            preferences.remove(stringKey)
            preferences.remove(booleanKey)
        }
    }

    /**
     * すべての設定をクリア
     */
    actual suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
