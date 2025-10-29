package net.chasmine.oneline.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.preferences.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager.getInstance(application)
    private val gitRepository = GitRepository.getInstance(application)

    val gitRepoUrl = settingsManager.gitRepoUrl
    val gitUsername = settingsManager.gitUsername

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                val repoUrl = settingsManager.gitRepoUrl.first()
                val username = settingsManager.gitUsername.first()
                val token = settingsManager.gitToken.first()
                val commitUserName = settingsManager.gitCommitUserName.first()
                val commitUserEmail = settingsManager.gitCommitUserEmail.first()

                _uiState.value = UiState.Loaded(
                    repoUrl = repoUrl,
                    username = username,
                    token = token,
                    commitUserName = commitUserName,
                    commitUserEmail = commitUserEmail
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load settings")
            }
        }
    }

    fun saveSettings(
        repoUrl: String,
        username: String,
        token: String,
        commitUserName: String = "",
        commitUserEmail: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving

            try {
                // 設定を保存
                settingsManager.saveGitSettings(repoUrl, username, token, commitUserName, commitUserEmail)

                // リポジトリを初期化
                val result = gitRepository.initRepository(repoUrl, username, token)

                result.fold(
                    onSuccess = {
                        _uiState.value = UiState.SaveSuccess
                    },
                    onFailure = { e ->
                        _uiState.value = UiState.Error(e.message ?: "Repository initialization failed")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to save settings")
            }
        }
    }

    fun validateRepository(repoUrl: String, username: String, token: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Validating

            try {
                // 安全な検証方式を使用（クローンなし）
                val validationResult = gitRepository.validateRepositorySafely(repoUrl, username, token)
                
                val message = when (validationResult) {
                    GitRepository.ValidationResult.DIARY_REPOSITORY -> 
                        "✅ 日記リポジトリとして最適です。\n\n日記形式のMarkdownファイル（YYYY-MM-DD.md）が確認されました。安全に使用できます。"
                    GitRepository.ValidationResult.LIKELY_DIARY_REPOSITORY -> 
                        "✅ 日記用リポジトリとして適切です。\n\nリポジトリ名またはMarkdownファイルから日記用途として判定されました。安全に使用できます。"
                    GitRepository.ValidationResult.EMPTY_REPOSITORY -> 
                        "✅ 空のリポジトリです。\n\n新しい日記リポジトリとして使用できます。初回同期時に日記ファイルが作成されます。"
                    GitRepository.ValidationResult.UNKNOWN_REPOSITORY -> 
                        "⚠️ 内容が不明なリポジトリです。\n\nリポジトリ名とファイル内容から用途を判定できませんでした。使用前にGitHub上で内容を確認することを推奨します。\n\n推奨: 「my-diary」「daily-notes」などの分かりやすい名前のリポジトリを作成してください。"
                    GitRepository.ValidationResult.SUSPICIOUS_REPOSITORY -> 
                        "❌ 危険: 開発用リポジトリの可能性があります。\n\nリポジトリ名から開発用途と判定されました。日記用には使用しないでください。\n\n推奨: 新しく日記専用のリポジトリを作成してください。\n例: 「my-diary-2025」「daily-journal」など"
                    GitRepository.ValidationResult.DANGEROUS_REPOSITORY -> 
                        "❌ 危険: 開発用リポジトリです。\n\nコードファイル（.kt, .java, .gradle等）が確認されました。このリポジトリは絶対に使用しないでください。\n\n推奨: 新しく日記専用のリポジトリを作成してください。"
                    GitRepository.ValidationResult.OWNERSHIP_VERIFICATION_FAILED -> 
                        "🚨 セキュリティ警告: リポジトリ所有者確認に失敗しました。\n\nこのリポジトリはあなたが所有していない可能性があります。他人のリポジトリに日記を投稿することは以下のリスクがあります：\n\n• プライバシーの侵害\n• 不正なデータ投稿\n• アカウントの悪用\n\n必ず自分が所有するリポジトリのみを使用してください。"
                    GitRepository.ValidationResult.AUTHENTICATION_FAILED -> 
                        "❌ 認証に失敗しました。\n\n以下を確認してください:\n• ユーザー名が正しいか\n• アクセストークンが有効か\n• トークンにリポジトリへの読み書き権限があるか"
                    GitRepository.ValidationResult.REPOSITORY_NOT_FOUND -> 
                        "❌ リポジトリが見つかりません。\n\n以下を確認してください:\n• URLが正しいか\n• リポジトリが存在するか\n• プライベートリポジトリの場合、アクセス権限があるか"
                    GitRepository.ValidationResult.CONNECTION_FAILED -> 
                        "❌ リポジトリに接続できませんでした。\n\n以下を確認してください:\n• インターネット接続\n• GitHubのサービス状況\n• ファイアウォール設定"
                    GitRepository.ValidationResult.VALIDATION_FAILED -> 
                        "❌ 検証に失敗しました。\n\n設定を確認して再度お試しください。問題が続く場合は、新しいリポジトリの作成を検討してください。"
                }
                
                _uiState.value = UiState.ValidationResult(
                    result = validationResult,
                    message = message
                )
                
            } catch (e: Exception) {
                _uiState.value = UiState.ValidationResult(
                    result = GitRepository.ValidationResult.VALIDATION_FAILED,
                    message = "検証中にエラーが発生しました: ${e.message}"
                )
            }
        }
    }

    fun migrateRepository(
        repoUrl: String,
        username: String,
        token: String,
        migrationOption: String,
        commitUserName: String = "",
        commitUserEmail: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving

            try {
                val option = when (migrationOption) {
                    "MIGRATE_DATA" -> GitRepository.MigrationOption.MIGRATE_DATA
                    "DISCARD_AND_SWITCH" -> GitRepository.MigrationOption.DISCARD_AND_SWITCH
                    else -> GitRepository.MigrationOption.DISCARD_AND_SWITCH
                }

                // 設定を保存
                settingsManager.saveGitSettings(repoUrl, username, token, commitUserName, commitUserEmail)

                // リポジトリ移行を実行
                val result = gitRepository.migrateToNewRepository(repoUrl, username, token, option)

                result.fold(
                    onSuccess = {
                        _uiState.value = UiState.SaveSuccess
                    },
                    onFailure = { e ->
                        _uiState.value = UiState.Error(e.message ?: "Repository migration failed")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to migrate repository")
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(
            val repoUrl: String,
            val username: String,
            val token: String,
            val commitUserName: String = "",
            val commitUserEmail: String = ""
        ) : UiState()
        object Saving : UiState()
        object SaveSuccess : UiState()
        data class Error(val message: String) : UiState()
        object Validating : UiState()
        data class ValidationResult(val result: GitRepository.ValidationResult, val message: String) : UiState()
        object Migrating : UiState()
        data class MigrationSuccess(val message: String) : UiState()
    }
}