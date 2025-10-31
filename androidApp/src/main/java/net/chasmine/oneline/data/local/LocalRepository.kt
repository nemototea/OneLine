package net.chasmine.oneline.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.chasmine.oneline.data.model.DiaryEntry
import kotlinx.datetime.LocalDate
import java.io.File

/**
 * ローカル専用のリポジトリクラス
 * 端末内のファイルシステムにのみ日記データを保存する
 */
class LocalRepository private constructor(private val context: Context) {

    private val TAG = "LocalRepository"
    private var localDirectory: File? = null
    private var isInitialized = false

    companion object {
        @Volatile
        private var INSTANCE: LocalRepository? = null

        fun getInstance(context: Context): LocalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * ローカルリポジトリの初期化
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true

            // アプリ専用のディレクトリに日記フォルダを作成
            localDirectory = File(context.filesDir, "diary_entries")
            
            if (!localDirectory!!.exists()) {
                val created = localDirectory!!.mkdirs()
                if (!created) {
                    Log.e(TAG, "Failed to create local directory")
                    return@withContext false
                }
            }

            isInitialized = true
            Log.d(TAG, "Local repository initialized: ${localDirectory!!.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize local repository", e)
            false
        }
    }

    /**
     * 日記エントリーを保存
     */
    suspend fun saveEntry(entry: DiaryEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized && !initialize()) {
                return@withContext false
            }

            val fileName = "${entry.date}.txt"
            val file = File(localDirectory, fileName)
            
            file.writeText(entry.content)
            Log.d(TAG, "Entry saved locally: $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save entry locally", e)
            false
        }
    }

    /**
     * 特定の日付の日記エントリーを取得
     */
    suspend fun getEntry(date: String): DiaryEntry? = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized && !initialize()) {
                return@withContext null
            }

            val file = File(localDirectory, "$date.txt")
            if (!file.exists()) {
                return@withContext null
            }

            val content = file.readText()
            val localDate = LocalDate.parse(date)
            DiaryEntry(date = localDate, content = content)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get entry locally: $date", e)
            null
        }
    }

    /**
     * すべての日記エントリーを取得
     */
    fun getAllEntries(): Flow<List<DiaryEntry>> = flow {
        try {
            if (!isInitialized && !initialize()) {
                emit(emptyList())
                return@flow
            }

            val entries = mutableListOf<DiaryEntry>()
            val files = localDirectory!!.listFiles { file ->
                file.isFile && file.name.endsWith(".txt")
            }

            files?.forEach { file ->
                try {
                    val dateString = file.nameWithoutExtension
                    val content = file.readText()
                    val localDate = LocalDate.parse(dateString)
                    entries.add(DiaryEntry(date = localDate, content = content))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse entry file: ${file.name}", e)
                }
            }

            // 日付順でソート（新しい順）
            entries.sortByDescending { it.date }
            emit(entries)
            Log.d(TAG, "Loaded ${entries.size} entries from local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all entries locally", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 日記エントリーを削除
     */
    suspend fun deleteEntry(date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized && !initialize()) {
                return@withContext false
            }

            val file = File(localDirectory, "$date.txt")
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Entry deleted locally: $date, success: $deleted")
                return@withContext deleted
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete entry locally: $date", e)
            false
        }
    }

    /**
     * ローカルデータをGitRepositoryに移行
     */
    suspend fun migrateToGitRepository(gitRepository: net.chasmine.oneline.data.git.GitRepository): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized && !initialize()) {
                return@withContext false
            }

            val entries = mutableListOf<DiaryEntry>()
            getAllEntries().collect { entryList ->
                entries.addAll(entryList)
            }

            var successCount = 0
            for (entry in entries) {
                try {
                    val result = gitRepository.saveEntry(entry)
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        Log.w(TAG, "Failed to migrate entry: ${entry.date}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while migrating entry: ${entry.date}", e)
                }
            }

            Log.d(TAG, "Migration completed: $successCount/${entries.size} entries migrated")
            successCount == entries.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate to Git repository", e)
            false
        }
    }

    /**
     * ローカルデータの整合性をチェック
     */
    suspend fun checkDataIntegrity(): DataIntegrityResult = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized && !initialize()) {
                return@withContext DataIntegrityResult.InitializationFailed
            }

            val files = localDirectory!!.listFiles { file ->
                file.isFile && file.name.endsWith(".txt")
            }

            if (files == null) {
                return@withContext DataIntegrityResult.DirectoryAccessFailed
            }

            var validEntries = 0
            var invalidEntries = 0
            val corruptedFiles = mutableListOf<String>()

            files.forEach { file ->
                try {
                    val dateString = file.nameWithoutExtension
                    // 日付形式の検証
                    LocalDate.parse(dateString)

                    // ファイル内容の検証
                    val content = file.readText()
                    if (content.isNotEmpty()) {
                        validEntries++
                    } else {
                        Log.w(TAG, "Empty content in file: ${file.name}")
                        invalidEntries++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Corrupted file detected: ${file.name}", e)
                    corruptedFiles.add(file.name)
                    invalidEntries++
                }
            }

            Log.d(TAG, "Data integrity check: $validEntries valid, $invalidEntries invalid entries")
            
            DataIntegrityResult.Success(
                totalFiles = files.size,
                validEntries = validEntries,
                invalidEntries = invalidEntries,
                corruptedFiles = corruptedFiles
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check data integrity", e)
            DataIntegrityResult.CheckFailed(e.message ?: "Unknown error")
        }
    }

    /**
     * 破損したファイルを修復または削除
     */
    suspend fun repairCorruptedFiles(corruptedFiles: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            var repairedCount = 0
            corruptedFiles.forEach { fileName ->
                val file = File(localDirectory, fileName)
                if (file.exists()) {
                    if (file.delete()) {
                        repairedCount++
                        Log.d(TAG, "Deleted corrupted file: $fileName")
                    }
                }
            }
            Log.d(TAG, "Repaired $repairedCount corrupted files")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to repair corrupted files", e)
            false
        }
    }

    /**
     * データ整合性チェック結果
     */
    sealed class DataIntegrityResult {
        data class Success(
            val totalFiles: Int,
            val validEntries: Int,
            val invalidEntries: Int,
            val corruptedFiles: List<String>
        ) : DataIntegrityResult()
        
        object InitializationFailed : DataIntegrityResult()
        object DirectoryAccessFailed : DataIntegrityResult()
        data class CheckFailed(val message: String) : DataIntegrityResult()
    }
    suspend fun clearLocalData(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized && !initialize()) {
                return@withContext false
            }

            val files = localDirectory!!.listFiles()
            var deletedCount = 0
            files?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }

            Log.d(TAG, "Local data cleared: $deletedCount files deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear local data", e)
            false
        }
    }
}
