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
            Log.d(TAG, "Starting repository initialization...")

            // 既に初期化済みの場合はスキップ
            if (isInitialized && git != null && repoDirectory?.exists() == true) {
                Log.d(TAG, "Repository already initialized and valid")
                return@withContext Result.success(true)
            }

            repoDirectory = File(context.filesDir, "OneLine_repository")
            credentialsProvider = UsernamePasswordCredentialsProvider(username, password)

            Log.d(TAG, "Repository directory: ${repoDirectory!!.absolutePath}")
            Log.d(TAG, "Repository directory exists: ${repoDirectory!!.exists()}")

            if (!repoDirectory!!.exists() || !File(repoDirectory, ".git").exists()) {
                // .gitディレクトリがない場合は新規クローン
                Log.d(TAG, "Cloning repository from $remoteUrl")

                // 既存のディレクトリがあれば削除
                if (repoDirectory!!.exists()) {
                    repoDirectory!!.deleteRecursively()
                }
                repoDirectory!!.mkdirs()

                git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(repoDirectory)
                    .setCredentialsProvider(credentialsProvider)
                    .call()

                Log.d(TAG, "Repository cloned successfully")
            } else {
                // 既存のリポジトリを開く
                Log.d(TAG, "Opening existing repository at $repoDirectory")
                git = Git.open(repoDirectory)

                // 認証情報を更新
                git?.repository?.config?.apply {
                    setString("remote", "origin", "url", remoteUrl)
                    save()
                }

                Log.d(TAG, "Repository opened successfully")
            }

            isInitialized = true
            Log.d(TAG, "Repository initialization completed successfully")
            Result.success(true)

        } catch (e: GitAPIException) {
            Log.e(TAG, "Git API error during init", e)
            isInitialized = false
            git = null
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error during init", e)
            isInitialized = false
            git = null
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during repository init", e)
            isInitialized = false
            git = null
            Result.failure(e)
        }
    }

    /**
     * すべての日記エントリを取得
     */
    fun getAllEntries(): Flow<List<DiaryEntry>> = flow {
        val entries = mutableListOf<DiaryEntry>()

        try {

            if (repoDirectory?.exists() == true) {
                val mdFiles = repoDirectory!!.listFiles { file ->
                    file.isFile && file.name.endsWith(".md") &&
                    DateUtils.isValidDateFormat(file.nameWithoutExtension, "yyyy-MM-dd")
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
            // リポジトリが初期化されていない場合はnullを返す
            if (!isInitialized || repoDirectory == null) {
                Log.w(TAG, "Repository not initialized when getting entry for $dateStr")
                return@withContext null
            }

            val fileName = "$dateStr.md"
            val file = File(repoDirectory, fileName)

            Log.d(TAG, "Looking for entry file: ${file.absolutePath}, exists: ${file.exists()}")

            if (file.exists() && file.canRead()) {
                val content = file.readText()
                val localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)

                Log.d(TAG, "Entry found for $dateStr, content length: ${content.length}")

                return@withContext DiaryEntry(
                    date = localDate,
                    content = content,
                    lastModified = file.lastModified()
                )
            }

            Log.d(TAG, "No entry found for $dateStr")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entry for date $dateStr", e)
            null
        }
    }

    // 日記エントリを保存
    suspend fun saveEntry(entry: DiaryEntry): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null || repoDirectory == null) {
                Log.e(TAG, "Repository not properly initialized")
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            val fileName = entry.getFileName()
            val file = File(repoDirectory, fileName)

            Log.d(TAG, "Saving entry to: ${file.absolutePath}")

            // ディレクトリが存在することを確認
            if (!repoDirectory!!.exists()) {
                repoDirectory!!.mkdirs()
            }

            // ファイルに内容を書き込み
            file.writeText(entry.content)

            Log.d(TAG, "Content written to file, length: ${entry.content.length}")

            // Gitに変更を追加してコミット
            git?.add()?.addFilepattern(fileName)?.call()

            val commitMessage = if (entry.content.trim().isEmpty()) {
                "Delete entry for ${entry.date}"
            } else {
                "Update entry for ${entry.date}"
            }

            val commitResult = git?.commit()?.setMessage(commitMessage)?.call()
            Log.d(TAG, "Commit completed: ${commitResult?.id?.name}")

            try {
                // 通常push
                git?.push()?.setCredentialsProvider(credentialsProvider)?.call()
                Log.d(TAG, "Push completed normally")
                Result.success(true)
            } catch (e: Exception) {
                Log.w(TAG, "Push failed, trying force overwrite (ours strategy)", e)
                try {
                    // ours戦略でpull（常にローカルを優先）
                    val pullCmd = git?.pull()
                    pullCmd?.setStrategy(org.eclipse.jgit.merge.MergeStrategy.OURS)
                    pullCmd?.setCredentialsProvider(credentialsProvider)
                    pullCmd?.call()

                    // ★ここで必ずアプリ内容でファイルを上書きし直す
                    file.writeText(entry.content)
                    git?.add()?.addFilepattern(fileName)?.call()
                    git?.commit()?.setMessage("Force overwrite entry for ${entry.date}")?.call()

                    // 再度force push
                    git?.push()?.setCredentialsProvider(credentialsProvider)?.setForce(true)?.call()
                    Log.d(TAG, "Force push completed after conflict, always using app content")
                    Result.success(true)
                } catch (ex: Exception) {
                    Log.e(TAG, "Force push failed", ex)
                    Result.failure(ex)
                }
            }
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

            // ours戦略でpull（常にローカルを優先）
            val pullResult = git!!.pull()
                .setCredentialsProvider(credentialsProvider)
                .setStrategy(org.eclipse.jgit.merge.MergeStrategy.OURS)
                .call()

            // 競合マーカーが残っている.mdファイルを修正
            val repoDir = repoDirectory
            if (repoDir != null && repoDir.exists()) {
                val mdFiles = repoDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".md")
                }
                mdFiles?.forEach { file ->
                    val content = file.readText()
                    if (content.contains("<<<<<<<") || content.contains("=======") || content.contains(">>>>>>>")) {
                        // 競合マーカーを除去（今回は空ファイルにする例。必要ならアプリ内容で上書きも可）
                        file.writeText("")
                        git?.add()?.addFilepattern(file.name)?.call()
                    }
                }
            }

            // 競合マーカー除去コミット（何か変更があれば）
            if (git?.status()?.call()?.hasUncommittedChanges() == true) {
                git?.commit()?.setMessage("Remove conflict markers and force sync")?.call()
            }

            // force push
            val pushResult = git!!.push()
                .setCredentialsProvider(credentialsProvider)
                .setForce(true)
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
        val isValid = isInitialized &&
                git != null &&
                repoDirectory != null &&
                repoDirectory!!.exists() &&
                credentialsProvider != null

        Log.d(TAG, "Repository config valid: $isValid")
        return isValid
    }
}