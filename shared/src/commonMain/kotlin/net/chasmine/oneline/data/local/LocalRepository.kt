package net.chasmine.oneline.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.data.storage.FileStorage

/**
 * ローカル専用のリポジトリクラス（共通化版）
 * 端末内のファイルシステムにのみ日記データを保存する
 * FileStorageインターフェースを使用してプラットフォーム非依存
 */
class LocalRepository(private val fileStorage: FileStorage) {

    private val DIARY_DIRECTORY = "diary_entries"
    private var isInitialized = false

    /**
     * ローカルリポジトリの初期化
     */
    suspend fun initialize(): Boolean {
        return try {
            if (isInitialized) return true

            // 日記フォルダを作成
            val result = fileStorage.createDirectory(DIARY_DIRECTORY)
            isInitialized = result.isSuccess
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 日記エントリーを保存
     */
    suspend fun saveEntry(entry: DiaryEntry): Boolean {
        return try {
            if (!isInitialized && !initialize()) {
                return false
            }

            val fileName = "$DIARY_DIRECTORY/${entry.date}.txt"
            val result = fileStorage.writeFile(fileName, entry.content)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 特定の日付の日記エントリーを取得
     */
    suspend fun getEntry(date: String): DiaryEntry? {
        return try {
            if (!isInitialized && !initialize()) {
                return null
            }

            val fileName = "$DIARY_DIRECTORY/$date.txt"
            val result = fileStorage.readFile(fileName)

            if (result.isSuccess) {
                val content = result.getOrNull() ?: return null
                val localDate = LocalDate.parse(date)
                DiaryEntry(date = localDate, content = content)
            } else {
                null
            }
        } catch (e: Exception) {
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
            val filesResult = fileStorage.listFiles(DIARY_DIRECTORY)

            if (filesResult.isSuccess) {
                val files = filesResult.getOrNull() ?: emptyList()

                files.filter { it.endsWith(".txt") }.forEach { fileName ->
                    try {
                        val dateString = fileName.removeSuffix(".txt")
                        val contentResult = fileStorage.readFile("$DIARY_DIRECTORY/$fileName")

                        if (contentResult.isSuccess) {
                            val content = contentResult.getOrNull() ?: ""
                            val localDate = LocalDate.parse(dateString)
                            entries.add(DiaryEntry(date = localDate, content = content))
                        }
                    } catch (e: Exception) {
                        // ファイル解析エラーはスキップ
                    }
                }
            }

            // 日付順でソート（新しい順）
            entries.sortByDescending { it.date }
            emit(entries)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    /**
     * 日記エントリーを削除
     */
    suspend fun deleteEntry(date: String): Boolean {
        return try {
            if (!isInitialized && !initialize()) {
                return false
            }

            val fileName = "$DIARY_DIRECTORY/$date.txt"
            val result = fileStorage.deleteFile(fileName)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * ローカルデータの整合性をチェック
     */
    suspend fun checkDataIntegrity(): DataIntegrityResult {
        return try {
            if (!isInitialized && !initialize()) {
                return DataIntegrityResult.InitializationFailed
            }

            val filesResult = fileStorage.listFiles(DIARY_DIRECTORY)

            if (filesResult.isFailure) {
                return DataIntegrityResult.DirectoryAccessFailed
            }

            val files = filesResult.getOrNull() ?: emptyList()
            val txtFiles = files.filter { it.endsWith(".txt") }

            var validEntries = 0
            var invalidEntries = 0
            val corruptedFiles = mutableListOf<String>()

            txtFiles.forEach { fileName ->
                try {
                    val dateString = fileName.removeSuffix(".txt")
                    // 日付形式の検証
                    LocalDate.parse(dateString)

                    // ファイル内容の検証
                    val contentResult = fileStorage.readFile("$DIARY_DIRECTORY/$fileName")
                    if (contentResult.isSuccess) {
                        val content = contentResult.getOrNull() ?: ""
                        if (content.isNotEmpty()) {
                            validEntries++
                        } else {
                            invalidEntries++
                        }
                    } else {
                        corruptedFiles.add(fileName)
                        invalidEntries++
                    }
                } catch (e: Exception) {
                    corruptedFiles.add(fileName)
                    invalidEntries++
                }
            }

            DataIntegrityResult.Success(
                totalFiles = txtFiles.size,
                validEntries = validEntries,
                invalidEntries = invalidEntries,
                corruptedFiles = corruptedFiles
            )
        } catch (e: Exception) {
            DataIntegrityResult.CheckFailed(e.message ?: "Unknown error")
        }
    }

    /**
     * 破損したファイルを修復または削除
     */
    suspend fun repairCorruptedFiles(corruptedFiles: List<String>): Boolean {
        return try {
            var repairedCount = 0
            corruptedFiles.forEach { fileName ->
                val result = fileStorage.deleteFile("$DIARY_DIRECTORY/$fileName")
                if (result.isSuccess) {
                    repairedCount++
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * ローカルデータをクリア
     */
    suspend fun clearLocalData(): Boolean {
        return try {
            if (!isInitialized && !initialize()) {
                return false
            }

            val filesResult = fileStorage.listFiles(DIARY_DIRECTORY)
            if (filesResult.isFailure) {
                return false
            }

            val files = filesResult.getOrNull() ?: emptyList()
            var deletedCount = 0
            files.forEach { fileName ->
                val result = fileStorage.deleteFile("$DIARY_DIRECTORY/$fileName")
                if (result.isSuccess) {
                    deletedCount++
                }
            }

            true
        } catch (e: Exception) {
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
}
