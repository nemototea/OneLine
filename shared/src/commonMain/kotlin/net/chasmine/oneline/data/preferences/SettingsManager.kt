package net.chasmine.oneline.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.chasmine.oneline.ui.theme.ThemeMode

/**
 * 設定管理クラス（共通コード）
 *
 * SettingsStorageを使用して設定を保存・取得します。
 * シングルトンパターンを採用し、アプリ全体で一つのインスタンスを共有します。
 */
class SettingsManager(private val storage: SettingsStorage) {

    // 設定キー
    companion object {
        private const val KEY_GIT_REPO_URL = "git_repo_url"
        private const val KEY_GIT_USERNAME = "git_username"
        private const val KEY_GIT_TOKEN = "git_token"
        private const val KEY_GIT_COMMIT_USER_NAME = "git_commit_user_name"
        private const val KEY_GIT_COMMIT_USER_EMAIL = "git_commit_user_email"
        private const val KEY_LOCAL_ONLY_MODE = "local_only_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DEVELOPER_MODE = "developer_mode"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(storage: SettingsStorage): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(storage).also {
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

    // Git設定を保存
    suspend fun saveGitSettings(
        repoUrl: String,
        username: String,
        token: String,
        commitUserName: String = "",
        commitUserEmail: String = ""
    ) {
        storage.saveString(KEY_GIT_REPO_URL, repoUrl)
        storage.saveString(KEY_GIT_USERNAME, username)
        storage.saveString(KEY_GIT_TOKEN, token)
        storage.saveString(KEY_GIT_COMMIT_USER_NAME, commitUserName)
        storage.saveString(KEY_GIT_COMMIT_USER_EMAIL, commitUserEmail)
        // Git設定を保存する際はローカルオンリーモードを無効にする
        storage.saveBoolean(KEY_LOCAL_ONLY_MODE, false)
    }

    // ローカルオンリーモードの設定
    suspend fun setLocalOnlyMode(enabled: Boolean) {
        storage.saveBoolean(KEY_LOCAL_ONLY_MODE, enabled)
        // ローカルオンリーモードを有効にする場合、Git設定をクリア
        if (enabled) {
            storage.remove(KEY_GIT_REPO_URL)
            storage.remove(KEY_GIT_USERNAME)
            storage.remove(KEY_GIT_TOKEN)
            storage.remove(KEY_GIT_COMMIT_USER_NAME)
            storage.remove(KEY_GIT_COMMIT_USER_EMAIL)
        }
    }

    // テーマモードの設定
    suspend fun setThemeMode(themeMode: ThemeMode) {
        storage.saveString(KEY_THEME_MODE, themeMode.name)
    }

    // 開発者モードの設定
    suspend fun setDeveloperMode(enabled: Boolean) {
        storage.saveBoolean(KEY_DEVELOPER_MODE, enabled)
    }

    // Git設定の各項目をFlowとして取得
    val gitRepoUrl: Flow<String> = storage.observeString(KEY_GIT_REPO_URL).map { it ?: "" }

    val gitUsername: Flow<String> = storage.observeString(KEY_GIT_USERNAME).map { it ?: "" }

    val gitToken: Flow<String> = storage.observeString(KEY_GIT_TOKEN).map { it ?: "" }

    val gitCommitUserName: Flow<String> = storage.observeString(KEY_GIT_COMMIT_USER_NAME).map { it ?: "" }

    val gitCommitUserEmail: Flow<String> = storage.observeString(KEY_GIT_COMMIT_USER_EMAIL).map { it ?: "" }

    // ローカルオンリーモードの状態
    val isLocalOnlyMode: Flow<Boolean> = storage.observeBoolean(KEY_LOCAL_ONLY_MODE)

    // テーマモードの取得
    val themeMode: Flow<ThemeMode> = storage.observeString(KEY_THEME_MODE).map { themeName ->
        themeName?.let {
            try {
                ThemeMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        } ?: ThemeMode.SYSTEM
    }

    // 開発者モードの状態
    val isDeveloperMode: Flow<Boolean> = storage.observeBoolean(KEY_DEVELOPER_MODE)

    // 設定が有効かどうかをチェック（ローカルオンリーモードまたはGit設定が完了）
    val hasValidSettings: Flow<Boolean> = combine(
        isLocalOnlyMode,
        gitRepoUrl,
        gitUsername,
        gitToken
    ) { isLocalOnly, repoUrl, username, token ->
        if (isLocalOnly) {
            // ローカルオンリーモードの場合は常に有効
            true
        } else {
            // Git連携モードの場合は全項目が必要
            repoUrl.isNotBlank() && username.isNotBlank() && token.isNotBlank()
        }
    }
}
