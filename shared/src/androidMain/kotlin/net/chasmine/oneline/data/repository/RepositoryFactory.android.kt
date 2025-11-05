package net.chasmine.oneline.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.preferences.SettingsStorage
import net.chasmine.oneline.data.storage.FileStorage
import net.chasmine.oneline.data.local.LocalRepository

/**
 * Android用のリポジトリファクトリー実装
 *
 * ローカルオンリーモードとGit連携モードを切り替えて適切なリポジトリを使用します。
 * Note: Git連携機能は現在androidAppのGitRepositoryに依存しています。
 * 完全なKMP対応はPhase 5で予定されています。
 */
actual class RepositoryFactory private constructor(private val context: Context) {

    private val TAG = "RepositoryFactory"
    private val settingsStorage = SettingsStorage(context)
    private val settingsManager = SettingsManager.getInstance(settingsStorage)
    private val fileStorage = FileStorage(context)
    private val localRepository = LocalRepository(fileStorage)

    // TODO: GitRepository統合は後のフェーズで実装
    // 現在はローカルリポジトリのみサポート

    actual suspend fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing repository")
            localRepository.initialize()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize repository", e)
            false
        }
    }

    actual suspend fun saveEntry(entry: DiaryEntry): Boolean {
        return try {
            localRepository.saveEntry(entry)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save entry", e)
            false
        }
    }

    actual suspend fun getEntry(date: String): DiaryEntry? {
        return try {
            localRepository.getEntry(date)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get entry", e)
            null
        }
    }

    actual fun getAllEntries(): Flow<List<DiaryEntry>> = flow {
        try {
            localRepository.getAllEntries().collect { entries ->
                emit(entries)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all entries", e)
            emit(emptyList())
        }
    }

    actual suspend fun deleteEntry(date: String): Boolean {
        return try {
            localRepository.deleteEntry(date)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete entry", e)
            false
        }
    }

    actual suspend fun syncRepository(): Boolean {
        Log.d(TAG, "Sync not supported in current implementation")
        return true
    }

    actual suspend fun getCurrentMode(): RepositoryMode {
        return RepositoryMode.LocalOnly
    }

    actual suspend fun hasValidSettings(): Boolean {
        return true // ローカルモードは常に有効
    }

    actual suspend fun migrateToGitMode(): MigrationResult {
        // TODO: Git連携機能の完全な移行はPhase 5で実装予定
        // 現在はRepositoryManager（androidApp）に依存しているため、ここでは未実装
        Log.d(TAG, "migrateToGitMode not yet supported in RepositoryFactory")
        return MigrationResult.UnknownError("Migration not yet supported in shared module. Use RepositoryManager directly.")
    }

    actual suspend fun migrateToLocalMode(clearGitData: Boolean): MigrationResult {
        // TODO: Git連携機能の完全な移行はPhase 5で実装予定
        // 現在はRepositoryManager（androidApp）に依存しているため、ここでは未実装
        Log.d(TAG, "migrateToLocalMode not yet supported in RepositoryFactory")
        return MigrationResult.UnknownError("Migration not yet supported in shared module. Use RepositoryManager directly.")
    }

    actual companion object {
        @Volatile
        private var INSTANCE: RepositoryFactory? = null

        actual fun create(): RepositoryFactory {
            throw UnsupportedOperationException(
                "RepositoryFactory.create() requires Android Context. " +
                "Use create(context) instead."
            )
        }

        fun create(context: Context): RepositoryFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RepositoryFactory(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
