package net.chasmine.oneline.data.preferences

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStoreのシングルトンインスタンスを作成
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 設定管理クラス
 * Gitの設定をDataStoreに保存・取得するためのクラス
 * ウィジェットとの連携を考慮して、シングルトンパターンを採用
 */
class SettingsManager(private val context: Context) {
    private val gitRepoUrlKey = stringPreferencesKey("git_repo_url")
    private val gitUsernameKey = stringPreferencesKey("git_username")
    private val gitTokenKey = stringPreferencesKey("git_token")
    private val localOnlyModeKey = booleanPreferencesKey("local_only_mode")

    // Git設定を保存
    suspend fun saveGitSettings(repoUrl: String, username: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[gitRepoUrlKey] = repoUrl
            preferences[gitUsernameKey] = username
            preferences[gitTokenKey] = token
            // Git設定を保存する際はローカルオンリーモードを無効にする
            preferences[localOnlyModeKey] = false
        }
    }
    
    // ローカルオンリーモードの設定
    suspend fun setLocalOnlyMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[localOnlyModeKey] = enabled
            // ローカルオンリーモードを有効にする場合、Git設定をクリア
            if (enabled) {
                preferences.remove(gitRepoUrlKey)
                preferences.remove(gitUsernameKey)
                preferences.remove(gitTokenKey)
            }
        }
    }

    // Git設定の各項目をFlowとして取得
    val gitRepoUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[gitRepoUrlKey] ?: ""
    }

    val gitUsername: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[gitUsernameKey] ?: ""
    }

    val gitToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[gitTokenKey] ?: ""
    }
    
    // ローカルオンリーモードの状態
    val isLocalOnlyMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[localOnlyModeKey] ?: false
    }

    // 設定が有効かどうかをチェック（ローカルオンリーモードまたはGit設定が完了）
    val hasValidSettings: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val isLocalOnly = preferences[localOnlyModeKey] ?: false
        if (isLocalOnly) {
            // ローカルオンリーモードの場合は常に有効
            true
        } else {
            // Git連携モードの場合は全項目が必要
            val repoUrl = preferences[gitRepoUrlKey] ?: ""
            val username = preferences[gitUsernameKey] ?: ""
            val token = preferences[gitTokenKey] ?: ""

            repoUrl.isNotBlank() && username.isNotBlank() && token.isNotBlank()
        }
    }

    companion object {
        // ウィジェットから常に参照されるのでメモリリーク警告を抑制
        // アプリケーションコンテキストを使用しているため、メモリリークの心配はない
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        // 明示的なクリーンアップメソッド
        fun destroyInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}