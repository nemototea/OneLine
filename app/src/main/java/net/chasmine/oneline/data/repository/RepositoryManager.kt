package net.chasmine.oneline.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.local.LocalRepository
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.data.preferences.SettingsManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * リポジトリ統合管理クラス
 * ローカルオンリーモードとGit連携モードを切り替えて適切なリポジトリを使用する
 */
class RepositoryManager private constructor(private val context: Context) {

    private val TAG = "RepositoryManager"
    private val settingsManager = SettingsManager.getInstance(context)
    private val gitRepository = GitRepository.getInstance(context)
    private val localRepository = LocalRepository.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: RepositoryManager? = null

        fun getInstance(context: Context): RepositoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RepositoryManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * 現在のモードに応じたリポジトリを初期化
     */
    suspend fun initialize(): Boolean {
        return try {
            val isLocalOnly = settingsManager.isLocalOnlyMode.first()
            if (isLocalOnly) {
                Log.d(TAG, "Initializing in local-only mode")
                localRepository.initialize()
            } else {
                Log.d(TAG, "Initializing in Git mode")
                // Git連携モードの場合は設定が必要
                val repoUrl = settingsManager.gitRepoUrl.first()
                val username = settingsManager.gitUsername.first()
                val token = settingsManager.gitToken.first()
                
                if (repoUrl.isNotBlank() && username.isNotBlank() && token.isNotBlank()) {
                    val result = gitRepository.initRepository(repoUrl, username, token)
                    result.isSuccess
                } else {
                    Log.w(TAG, "Git settings not configured")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize repository", e)
            false
        }
    }

    /**
     * 日記エントリーを保存
     */
    suspend fun saveEntry(entry: DiaryEntry): Boolean {
        return try {
            val isLocalOnly = settingsManager.isLocalOnlyMode.first()
            if (isLocalOnly) {
                localRepository.saveEntry(entry)
            } else {
                val result = gitRepository.saveEntry(entry)
                result.isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save entry", e)
            false
        }
    }

    /**
     * 特定の日付の日記エントリーを取得
     */
    suspend fun getEntry(date: String): DiaryEntry? {
        return try {
            val isLocalOnly = settingsManager.isLocalOnlyMode.first()
            if (isLocalOnly) {
                localRepository.getEntry(date)
            } else {
                gitRepository.getEntry(date)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get entry", e)
            null
        }
    }

    /**
     * 特定の月の日記エントリーがある日付を取得
     */
    suspend fun getDiaryEntriesForMonth(yearMonth: YearMonth): Set<LocalDate> {
        return try {
            val isLocalOnly = settingsManager.isLocalOnlyMode.first()
            val allEntries = if (isLocalOnly) {
                localRepository.getAllEntries().first()
            } else {
                gitRepository.getAllEntries().first()
            }
            
            allEntries.mapNotNull { entry ->
                try {
                    val entryDate = LocalDate.parse(entry.date)
                    if (entryDate.year == yearMonth.year && entryDate.monthValue == yearMonth.monthValue) {
                        entryDate
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse date: ${entry.date}", e)
                    null
                }
            }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get diary entries for month", e)
            emptySet()
        }
    }

    /**
     * すべての日記エントリーを取得
     */
    fun getAllEntries(): Flow<List<DiaryEntry>> = flow {
        try {
            val isLocalOnly = settingsManager.isLocalOnlyMode.first()
            if (isLocalOnly) {
                localRepository.getAllEntries().collect { entries ->
                    emit(entries)
                }
            } else {
                gitRepository.getAllEntries().collect { entries ->
                    emit(entries)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all entries", e)
            emit(emptyList())
        }
    }

    /**
     * 日記エントリーを削除
     */
    suspend fun deleteEntry(date: String): Boolean {
        return try {
            val isLocalOnly = settingsManager.isLocalOnlyMode.first()
            if (isLocalOnly) {
                localRepository.deleteEntry(date)
            } else {
                // GitRepositoryのdeleteEntryはDiaryEntryを受け取るので、まずエントリーを取得
                val entry = gitRepository.getEntry(date)
                if (entry != null) {
                    val result = gitRepository.deleteEntry(entry)
                    result.isSuccess
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete entry", e)
            false
        }
    }

    /**
     * 同期処理（Git連携モードのみ）
     */
    suspend fun syncRepository(): Boolean {
        return try {
            val isLocalOnly = settingsManager.isLocalOnlyMode.first()
            if (isLocalOnly) {
                Log.d(TAG, "Sync skipped: local-only mode")
                true // ローカルオンリーモードでは同期は不要
            } else {
                val result = gitRepository.syncRepository()
                result.isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync repository", e)
            false
        }
    }

    /**
     * ローカルオンリーモードからGit連携モードへの移行
     */
    suspend fun migrateToGitMode(): MigrationResult {
        return try {
            Log.d(TAG, "Starting migration from local-only to Git mode")
            
            // Git設定を確認
            val repoUrl = settingsManager.gitRepoUrl.first()
            val username = settingsManager.gitUsername.first()
            val token = settingsManager.gitToken.first()
            
            if (repoUrl.isBlank() || username.isBlank() || token.isBlank()) {
                Log.w(TAG, "Git settings not configured for migration")
                return MigrationResult.GitSettingsNotConfigured
            }
            
            // Git リポジトリを初期化
            val initResult = gitRepository.initRepository(repoUrl, username, token)
            if (initResult.isFailure) {
                Log.e(TAG, "Git repository initialization failed", initResult.exceptionOrNull())
                return MigrationResult.GitInitializationFailed
            }

            // ローカルデータをGitリポジトリに移行
            val migrationSuccess = localRepository.migrateToGitRepository(gitRepository)
            if (!migrationSuccess) {
                Log.e(TAG, "Data migration from local to Git failed")
                return MigrationResult.DataMigrationFailed
            }

            // 設定をGit連携モードに変更（これによりローカルオンリーモードが無効になる）
            settingsManager.saveGitSettings(repoUrl, username, token)
            
            Log.d(TAG, "Migration to Git mode completed successfully")
            MigrationResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate to Git mode", e)
            MigrationResult.UnknownError(e.message ?: "Unknown error")
        }
    }

    /**
     * Git連携モードからローカルオンリーモードへの移行
     */
    suspend fun migrateToLocalMode(clearGitData: Boolean = false): MigrationResult {
        return try {
            Log.d(TAG, "Starting migration from Git to local-only mode")
            
            // ローカルリポジトリを初期化
            if (!localRepository.initialize()) {
                return MigrationResult.LocalInitializationFailed
            }

            // Gitデータをローカルに移行
            gitRepository.getAllEntries().collect { entries ->
                for (entry in entries) {
                    if (!localRepository.saveEntry(entry)) {
                        Log.w(TAG, "Failed to migrate entry to local: ${entry.date}")
                    }
                }
            }

            // 設定をローカルオンリーモードに変更
            settingsManager.setLocalOnlyMode(true)
            
            if (clearGitData) {
                // Git設定をクリア（既にsetLocalOnlyModeで実行済み）
                Log.d(TAG, "Git settings cleared")
            }
            
            Log.d(TAG, "Migration to local-only mode completed successfully")
            MigrationResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate to local mode", e)
            MigrationResult.UnknownError(e.message ?: "Unknown error")
        }
    }

    /**
     * 現在のモードを取得
     */
    suspend fun getCurrentMode(): RepositoryMode {
        return try {
            val isLocalOnly = settingsManager.isLocalOnlyMode.first()
            if (isLocalOnly) RepositoryMode.LocalOnly else RepositoryMode.Git
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current mode", e)
            RepositoryMode.Git // デフォルトはGitモード
        }
    }

    /**
     * 設定が有効かどうかをチェック
     */
    suspend fun hasValidSettings(): Boolean {
        return try {
            settingsManager.hasValidSettings.first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check valid settings", e)
            false
        }
    }

    /**
     * リポジトリモード
     */
    enum class RepositoryMode {
        LocalOnly,
        Git
    }

    /**
     * 移行結果
     */
    sealed class MigrationResult {
        object Success : MigrationResult()
        object GitInitializationFailed : MigrationResult()
        object LocalInitializationFailed : MigrationResult()
        object DataMigrationFailed : MigrationResult()
        object GitSettingsNotConfigured : MigrationResult()
        data class UnknownError(val message: String) : MigrationResult()
        
        fun getErrorMessage(): String = when (this) {
            is Success -> "移行が完了しました"
            is GitInitializationFailed -> "Git リポジトリの初期化に失敗しました。設定を確認してください。"
            is LocalInitializationFailed -> "ローカルストレージの初期化に失敗しました。"
            is DataMigrationFailed -> "データの移行に失敗しました。一部のデータが移行されていない可能性があります。"
            is GitSettingsNotConfigured -> "Git設定が完了していません。リポジトリURL、ユーザー名、トークンを設定してください。"
            is UnknownError -> "予期しないエラーが発生しました: $message"
        }
    }
}
