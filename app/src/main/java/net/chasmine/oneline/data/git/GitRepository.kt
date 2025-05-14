package net.chasmine.oneline.data.git

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.chasmine.oneline.data.model.DiaryEntry
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.util.DateUtils

class GitRepository private constructor(private val context: Context) {

    private val TAG = "GitRepository"
    private var git: Git? = null
    private var credentialsProvider: CredentialsProvider? = null
    private var repoDirectory: File? = null
    private var isInitialized = false

    companion object {
        // ウィジェットから常に参照されるのでメモリリーク警告を抑制
        // アプリケーションコンテキストを使用しているため、メモリリークの心配はない
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: GitRepository? = null

        fun getInstance(context: Context): GitRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GitRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * リポジトリの初期化
     * 初回：クローン
     * 2回目以降：オープン
     */
    suspend fun initRepository(
        remoteUrl: String,
        username: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            repoDirectory = File(context.filesDir, "OneLine_repository")
            credentialsProvider = UsernamePasswordCredentialsProvider(username, password)

            if (!repoDirectory!!.exists()) {
                Log.d(TAG, "Cloning repository from $remoteUrl")
                repoDirectory!!.mkdirs()
                git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(repoDirectory)
                    .setCredentialsProvider(credentialsProvider)
                    .call()
            } else {
                Log.d(TAG, "Repository already exists at $repoDirectory")
                git = Git.open(repoDirectory)
            }

            isInitialized = true
            Result.success(true)
        } catch (e: GitAPIException) {
            Log.e(TAG, "Git API error during init", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error during init", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during repository init", e)
            Result.failure(e)
        }
    }

    /**
     * すべての日記エントリを取得
     */
    fun getAllEntries(dateUtils: DateUtils): Flow<List<DiaryEntry>> = flow {
        val entries = mutableListOf<DiaryEntry>()

        try {

            if (repoDirectory?.exists() == true) {
                val mdFiles = repoDirectory!!.listFiles { file ->
                    file.isFile && file.name.endsWith(".md") &&
                    dateUtils.isValidDateFormat(file.nameWithoutExtension, "yyyy-MM-dd")
                }

                mdFiles?.forEach { file ->
                    try {
                        val fileName = file.name
                        val dateStr = fileName.substringBefore(".md")
                        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)

                        val content = file.readText()

                        entries.add(DiaryEntry(
                            date = date,
                            content = content,
                            lastModified = file.lastModified()
                        ))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing file ${file.name}", e)
                    }
                }

                entries.sortByDescending { it.date }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading entries", e)
        }

        emit(entries)
    }.flowOn(Dispatchers.IO)

    /**
     * 特定の日付の日記エントリを取得
     */
    suspend fun getEntry(dateStr: String): DiaryEntry? = withContext(Dispatchers.IO) {
        try {
            val fileName = "$dateStr.md"
            val file = File(repoDirectory, fileName)

            if (file.exists()) {
                val content = file.readText()
                val localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                return@withContext DiaryEntry(
                    date = localDate,
                    content = content,
                    lastModified = file.lastModified()
                )
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entry for date $dateStr", e)
            null
        }
    }

    // 日記エントリを保存
    suspend fun saveEntry(entry: DiaryEntry): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            val fileName = entry.getFileName()
            val file = File(repoDirectory, fileName)

            // ファイルに内容を書き込み
            file.writeText(entry.content)

            // Gitに変更を追加してコミット
            git?.add()?.addFilepattern(fileName)?.call()

            val commitMessage = if (file.length() == 0L) {
                "Delete entry for ${entry.date}"
            } else {
                "Update entry for ${entry.date}"
            }

            git?.commit()?.setMessage(commitMessage)?.call()

            Result.success(true)
        } catch (e: GitAPIException) {
            Log.e(TAG, "Git API error during save", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error during save", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during save", e)
            Result.failure(e)
        }
    }

    // 日記エントリを削除
    suspend fun deleteEntry(entry: DiaryEntry): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            val fileName = entry.getFileName()
            val file = File(repoDirectory, fileName)

            if (file.exists()) {
                file.delete()

                // Gitから削除をステージング
                git?.rm()?.addFilepattern(fileName)?.call()

                // コミット
                git?.commit()?.setMessage("Delete entry for ${entry.date}")?.call()

                return@withContext Result.success(true)
            }

            Result.failure(Exception("File does not exist"))
        } catch (e: GitAPIException) {
            Log.e(TAG, "Git API error during delete", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error during delete", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during delete", e)
            Result.failure(e)
        }
    }

    /**
     * リモートからプル
     */
    suspend fun syncRepository(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null || credentialsProvider == null) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            // まずプル
            val pullResult = git!!.pull()
                .setCredentialsProvider(credentialsProvider)
                .call()

            // 次にプッシュ
            val pushResult = git!!.push()
                .setCredentialsProvider(credentialsProvider)
                .call()

            Log.d(TAG, "Pull result: ${pullResult.mergeResult?.mergeStatus}")
            Log.d(TAG, "Push result: $pushResult")

            Result.success(true)
        } catch (e: GitAPIException) {
            Log.e(TAG, "Git API error during sync", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    /**
     * リポジトリの初期化状態を確認
     */
    fun isConfigValid(): Boolean {
        return isInitialized
    }
}