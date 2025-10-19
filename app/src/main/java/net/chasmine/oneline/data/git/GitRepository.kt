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
import org.eclipse.jgit.lib.PersonIdent

class GitRepository private constructor(private val context: Context) {

    private val TAG = "GitRepository"
    private var git: Git? = null
    private var credentialsProvider: CredentialsProvider? = null
    private var repoDirectory: File? = null
    private var isInitialized = false
    private val settingsManager = SettingsManager.getInstance(context)

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
            Log.d(TAG, "Target remote URL: $remoteUrl")

            // 既存のリポジトリがある場合、リモートURLを確認
            if (repoDirectory!!.exists() && File(repoDirectory, ".git").exists()) {
                try {
                    val existingGit = Git.open(repoDirectory)
                    val existingRemoteUrl = existingGit.repository.config.getString("remote", "origin", "url")
                    existingGit.close()
                    
                    Log.d(TAG, "Existing remote URL: $existingRemoteUrl")
                    
                    if (existingRemoteUrl != null && existingRemoteUrl != remoteUrl) {
                        Log.w(TAG, "Remote URL mismatch detected!")
                        Log.w(TAG, "Existing: $existingRemoteUrl")
                        Log.w(TAG, "New: $remoteUrl")
                        Log.w(TAG, "Removing existing repository for safety")
                        
                        // 既存のリポジトリを完全に削除
                        repoDirectory!!.deleteRecursively()
                        Log.d(TAG, "Existing repository removed successfully")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to check existing repository, removing for safety", e)
                    repoDirectory!!.deleteRecursively()
                }
            }

            if (!repoDirectory!!.exists() || !File(repoDirectory, ".git").exists()) {
                // .gitディレクトリがない場合は新規クローン
                Log.d(TAG, "Cloning repository from $remoteUrl")

                // 既存のディレクトリがあれば削除
                if (repoDirectory!!.exists()) {
                    Log.w(TAG, "Removing existing repository directory for safety")
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
                // 既存のリポジトリを開く前に、リモートURLが一致するかチェック
                Log.d(TAG, "Checking existing repository at $repoDirectory")
                val existingGit = Git.open(repoDirectory)
                val existingRemoteUrl = existingGit.repository.config.getString("remote", "origin", "url")
                
                Log.d(TAG, "Existing remote URL: $existingRemoteUrl")
                Log.d(TAG, "New remote URL: $remoteUrl")
                
                if (existingRemoteUrl != remoteUrl) {
                    Log.w(TAG, "Remote URL mismatch! Removing existing repository for safety")
                    existingGit.close()
                    repoDirectory!!.deleteRecursively()
                    repoDirectory!!.mkdirs()
                    
                    // 新しいリポジトリをクローン
                    git = Git.cloneRepository()
                        .setURI(remoteUrl)
                        .setDirectory(repoDirectory)
                        .setCredentialsProvider(credentialsProvider)
                        .call()
                    
                    Log.d(TAG, "Repository re-cloned with new URL")
                } else {
                    // URLが一致する場合のみ既存リポジトリを使用
                    git = existingGit
                    
                    // 認証情報を更新
                    git?.repository?.config?.apply {
                        setString("remote", "origin", "url", remoteUrl)
                        save()
                    }
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

            // コミットユーザー情報を取得
            val commitUserName = settingsManager.gitCommitUserName.first()
            val commitUserEmail = settingsManager.gitCommitUserEmail.first()

            // PersonIdentを設定（ユーザー情報が設定されている場合のみ）
            val commitCommand = git?.commit()?.setMessage(commitMessage)
            if (commitUserName.isNotBlank() && commitUserEmail.isNotBlank()) {
                val author = PersonIdent(commitUserName, commitUserEmail)
                commitCommand?.setAuthor(author)
                commitCommand?.setCommitter(author)
                Log.d(TAG, "Using commit author: $commitUserName <$commitUserEmail>")
            } else {
                Log.w(TAG, "Commit user info not set, using default Git identity")
            }

            val commitResult = commitCommand?.call()
            Log.d(TAG, "Commit completed: ${commitResult?.id?.name}")

            try {
                // 通常push
                git?.push()?.setCredentialsProvider(credentialsProvider)?.call()
                Log.d(TAG, "Push completed normally")
                Result.success(true)
            } catch (e: Exception) {
                Log.w(TAG, "Push failed, trying safe conflict resolution", e)
                try {
                    // 安全な競合解決戦略
                    Log.d(TAG, "Attempting safe merge strategy...")
                    
                    // まずpullして最新状態を取得
                    val pullCmd = git?.pull()
                    pullCmd?.setStrategy(org.eclipse.jgit.merge.MergeStrategy.OURS) // ローカルを優先
                    pullCmd?.setCredentialsProvider(credentialsProvider)
                    pullCmd?.call()
                    
                    // 日記ファイルの内容を確実に保持
                    val file = File(repoDirectory, fileName)
                    if (!file.exists() || file.readText().trim() != entry.content.trim()) {
                        file.writeText(entry.content)
                        git?.add()?.addFilepattern(fileName)?.call()

                        // PersonIdentを設定してコミット
                        val ensureCommitCommand = git?.commit()?.setMessage("Ensure diary content for ${entry.date}")
                        if (commitUserName.isNotBlank() && commitUserEmail.isNotBlank()) {
                            val author = PersonIdent(commitUserName, commitUserEmail)
                            ensureCommitCommand?.setAuthor(author)
                            ensureCommitCommand?.setCommitter(author)
                        }
                        ensureCommitCommand?.call()
                    }
                    
                    // 通常のpushを再試行
                    git?.push()?.setCredentialsProvider(credentialsProvider)?.call()
                    Log.d(TAG, "Safe merge and push completed successfully")
                    Result.success(true)
                    
                } catch (ex: Exception) {
                    Log.e(TAG, "Safe merge failed, this may indicate a serious conflict", ex)
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

                // コミットユーザー情報を取得
                val commitUserName = settingsManager.gitCommitUserName.first()
                val commitUserEmail = settingsManager.gitCommitUserEmail.first()

                // コミット（PersonIdentを設定）
                val deleteCommitCommand = git?.commit()?.setMessage("Delete entry for ${entry.date}")
                if (commitUserName.isNotBlank() && commitUserEmail.isNotBlank()) {
                    val author = PersonIdent(commitUserName, commitUserEmail)
                    deleteCommitCommand?.setAuthor(author)
                    deleteCommitCommand?.setCommitter(author)
                }
                deleteCommitCommand?.call()

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

            // MERGING状態なら強制リセット
            val repo = git!!.repository
            if (repo.repositoryState.toString().contains("MERGING")) {
                Log.w(TAG, "Repository in MERGING state, resetting hard")
                git!!.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call()
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
                    if (content.contains("<<<<<<<") || content.contains("=======" ) || content.contains(">>>>>>>")) {
                        file.writeText("")
                        git?.add()?.addFilepattern(file.name)?.call()
                    }
                }
            }

            // 競合マーカー除去コミット（何か変更があれば）
            if (git?.status()?.call()?.hasUncommittedChanges() == true) {
                // コミットユーザー情報を取得
                val commitUserName = settingsManager.gitCommitUserName.first()
                val commitUserEmail = settingsManager.gitCommitUserEmail.first()

                val syncCommitCommand = git?.commit()?.setMessage("Remove conflict markers and sync")
                if (commitUserName.isNotBlank() && commitUserEmail.isNotBlank()) {
                    val author = PersonIdent(commitUserName, commitUserEmail)
                    syncCommitCommand?.setAuthor(author)
                    syncCommitCommand?.setCommitter(author)
                }
                syncCommitCommand?.call()
            }

            // 通常のpushを試行
            try {
                val pushResult = git!!.push()
                    .setCredentialsProvider(credentialsProvider)
                    .call()
                
                Log.d(TAG, "Pull result: ${pullResult.mergeResult?.mergeStatus}")
                Log.d(TAG, "Push result: $pushResult")
                Log.d(TAG, "Sync completed successfully")
                
            } catch (pushException: Exception) {
                Log.w(TAG, "Push failed during sync, but pull was successful", pushException)
                // プルは成功しているので、プッシュ失敗は警告レベルで処理
            }

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

    /**
     * リポジトリが日記用として適切かどうかを安全に検証（セキュリティ強化版）
     */
    suspend fun validateRepositorySafely(remoteUrl: String, username: String, password: String): ValidationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting safe repository validation for: $remoteUrl")
            
            // 認証情報を設定
            val credentials = UsernamePasswordCredentialsProvider(username, password)
            
            // 1. リモートリポジトリへの接続テスト
            val refs = try {
                val lsRemoteCommand = Git.lsRemoteRepository()
                    .setRemote(remoteUrl)
                    .setCredentialsProvider(credentials)
                    .setHeads(true)
                    .setTags(false)
                
                lsRemoteCommand.call()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to access repository", e)
                return@withContext when {
                    e.message?.contains("not authorized") == true || 
                    e.message?.contains("authentication") == true -> 
                        ValidationResult.AUTHENTICATION_FAILED
                    e.message?.contains("not found") == true -> 
                        ValidationResult.REPOSITORY_NOT_FOUND
                    else -> ValidationResult.CONNECTION_FAILED
                }
            }
            
            Log.d(TAG, "Repository is accessible, found ${refs.size} refs")
            
            // 2. 空のリポジトリチェック
            if (refs.isEmpty()) {
                // 空のリポジトリでも所有者確認は必要
                val ownershipCheck = verifyRepositoryOwnership(remoteUrl, username)
                return@withContext if (ownershipCheck) {
                    ValidationResult.EMPTY_REPOSITORY
                } else {
                    ValidationResult.OWNERSHIP_VERIFICATION_FAILED
                }
            }
            
            // 3. リポジトリ所有者確認
            val isOwner = verifyRepositoryOwnership(remoteUrl, username)
            if (!isOwner) {
                Log.w(TAG, "Repository ownership verification failed for user: $username")
                return@withContext ValidationResult.OWNERSHIP_VERIFICATION_FAILED
            }
            
            // 4. ファイル一覧を取得（軽量な方法）
            val fileList = try {
                getRepositoryFileList(remoteUrl, credentials)
            } catch (e: Exception) {
                Log.w(TAG, "Could not get file list, falling back to name-based validation", e)
                emptyList<String>()
            }
            
            // 5. ファイル内容による判定
            val hasMarkdownFiles = fileList.any { it.endsWith(".md") }
            val hasDiaryFiles = fileList.any { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md")) }
            val hasCodeFiles = fileList.any { fileName ->
                fileName.endsWith(".kt") || fileName.endsWith(".java") || 
                fileName.endsWith(".gradle") || fileName == "build.gradle.kts" ||
                fileName.endsWith(".xml") && fileName.contains("android") ||
                fileName == "AndroidManifest.xml"
            }
            
            // 6. リポジトリ名による判定
            val repoName = remoteUrl.substringAfterLast("/").removeSuffix(".git").lowercase()
            val suspiciousPatterns = listOf(
                "oneline", "app", "android", "source", "code", "project", 
                "dev", "development", "src", "main", "build"
            )
            val diaryPatterns = listOf(
                "diary", "journal", "note", "obsidian", "vault", "daily", "log"
            )
            
            val nameIsSuspicious = suspiciousPatterns.any { repoName.contains(it) }
            val nameIsDiaryLike = diaryPatterns.any { repoName.contains(it) }
            
            // 7. 総合判定
            when {
                hasCodeFiles -> {
                    Log.w(TAG, "Repository contains code files - dangerous")
                    ValidationResult.DANGEROUS_REPOSITORY
                }
                hasDiaryFiles -> {
                    Log.d(TAG, "Repository contains diary files - safe")
                    ValidationResult.DIARY_REPOSITORY
                }
                nameIsSuspicious -> {
                    Log.w(TAG, "Repository name suggests development use: $repoName")
                    ValidationResult.SUSPICIOUS_REPOSITORY
                }
                nameIsDiaryLike || hasMarkdownFiles -> {
                    Log.d(TAG, "Repository appears suitable for diary use")
                    ValidationResult.LIKELY_DIARY_REPOSITORY
                }
                else -> {
                    Log.d(TAG, "Repository content is unknown: $repoName")
                    ValidationResult.UNKNOWN_REPOSITORY
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Repository validation failed", e)
            ValidationResult.VALIDATION_FAILED
        }
    }

    /**
     * リポジトリの所有者確認
     */
    private suspend fun verifyRepositoryOwnership(remoteUrl: String, username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // GitHubのURL形式から所有者を抽出
            val repoOwner = extractRepositoryOwner(remoteUrl)
            
            if (repoOwner == null) {
                Log.w(TAG, "Could not extract repository owner from URL: $remoteUrl")
                return@withContext false
            }
            
            // ユーザー名と所有者名を比較（大文字小文字を無視）
            val isOwner = username.equals(repoOwner, ignoreCase = true)
            
            Log.d(TAG, "Repository owner: $repoOwner, User: $username, Is owner: $isOwner")
            
            if (!isOwner) {
                Log.w(TAG, "User '$username' is not the owner of repository owned by '$repoOwner'")
            }
            
            isOwner
            
        } catch (e: Exception) {
            Log.e(TAG, "Repository ownership verification failed", e)
            false
        }
    }

    /**
     * リポジトリURLから所有者名を抽出
     */
    private fun extractRepositoryOwner(remoteUrl: String): String? {
        return try {
            // GitHub URL patterns:
            // https://github.com/username/repo.git
            // git@github.com:username/repo.git
            
            val patterns = listOf(
                Regex("https://github\\.com/([^/]+)/[^/]+(?:\\.git)?/?$"),
                Regex("git@github\\.com:([^/]+)/[^/]+(?:\\.git)?/?$")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(remoteUrl)
                if (match != null) {
                    return match.groupValues[1]
                }
            }
            
            Log.w(TAG, "Could not parse repository owner from URL: $remoteUrl")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting repository owner", e)
            null
        }
    }
    
    /**
     * リポジトリのファイル一覧を軽量に取得
     */
    private suspend fun getRepositoryFileList(remoteUrl: String, credentials: UsernamePasswordCredentialsProvider): List<String> = withContext(Dispatchers.IO) {
        // 一時的な浅いクローンでファイル一覧のみ取得
        val tempDir = File(context.cacheDir, "temp_validation_${System.currentTimeMillis()}")
        try {
            tempDir.mkdirs()
            
            val tempGit = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(tempDir)
                .setCredentialsProvider(credentials)
                .setDepth(1) // 浅いクローン（最新コミットのみ）
                .call()
            
            val files = tempDir.walkTopDown()
                .filter { it.isFile && !it.path.contains("/.git/") }
                .map { it.relativeTo(tempDir).path }
                .toList()
            
            tempGit.close()
            Log.d(TAG, "Found ${files.size} files in repository")
            
            files
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    enum class ValidationResult {
        DIARY_REPOSITORY,          // 日記ファイルを含む（最も安全）
        LIKELY_DIARY_REPOSITORY,   // 名前から日記用と推測（安全）
        EMPTY_REPOSITORY,          // 空のリポジトリ（安全）
        UNKNOWN_REPOSITORY,        // 不明な内容（注意）
        SUSPICIOUS_REPOSITORY,     // 開発用の可能性（危険）
        DANGEROUS_REPOSITORY,      // コードファイルを含む（最も危険）
        OWNERSHIP_VERIFICATION_FAILED, // 所有者確認失敗（セキュリティリスク）
        AUTHENTICATION_FAILED,     // 認証失敗
        REPOSITORY_NOT_FOUND,      // リポジトリが見つからない
        CONNECTION_FAILED,         // 接続失敗
        VALIDATION_FAILED          // 検証失敗
    }

    /**
     * リポジトリ移行オプション
     */
    enum class MigrationOption {
        MIGRATE_DATA,      // データを新しいリポジトリに移行（競合解決付き）
        DISCARD_AND_SWITCH // 既存データを破棄して新しいリポジトリに切り替え
    }

    /**
     * リポジトリ移行の実行
     */
    suspend fun migrateToNewRepository(
        newRemoteUrl: String, 
        newUsername: String, 
        newPassword: String,
        migrationOption: MigrationOption
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting repository migration to: $newRemoteUrl")
            
            // 1. 現在のローカルデータを取得
            val localDiaryFiles = getLocalDiaryFiles()
            Log.d(TAG, "Found ${localDiaryFiles.size} local diary files")
            
            // 2. 移行オプションに応じた処理
            when (migrationOption) {
                MigrationOption.MIGRATE_DATA -> {
                    migrateDataToNewRepository(newRemoteUrl, newUsername, newPassword, localDiaryFiles)
                }
                MigrationOption.DISCARD_AND_SWITCH -> {
                    discardAndSwitch(newRemoteUrl, newUsername, newPassword)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Repository migration failed", e)
            Result.failure(e)
        }
    }

    /**
     * ローカルの日記ファイル一覧を取得
     */
    private fun getLocalDiaryFiles(): List<DiaryFile> {
        if (repoDirectory == null || !repoDirectory!!.exists()) {
            return emptyList()
        }
        
        return repoDirectory!!.listFiles()
            ?.filter { it.isFile && it.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md")) }
            ?.map { file ->
                DiaryFile(
                    fileName = file.name,
                    content = file.readText(),
                    lastModified = file.lastModified()
                )
            } ?: emptyList()
    }

    /**
     * データを新しいリポジトリに移行（競合解決付き）
     */
    private suspend fun migrateDataToNewRepository(
        newRemoteUrl: String,
        newUsername: String, 
        newPassword: String,
        localFiles: List<DiaryFile>
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 1. 新しいリポジトリを初期化
            val result = initRepository(newRemoteUrl, newUsername, newPassword)
            if (result.isFailure) {
                return@withContext result
            }
            
            // 2. 既存ファイルとの競合をチェック
            val conflictResolution = checkAndResolveConflicts(localFiles)
            
            // 3. 競合解決されたファイルを新しいリポジトリに追加
            conflictResolution.filesToMigrate.forEach { diaryFile ->
                val file = File(repoDirectory, diaryFile.fileName)
                file.writeText(diaryFile.content)
                
                git?.add()?.addFilepattern(diaryFile.fileName)?.call()
            }
            
            // 4. 移行コミット
            if (conflictResolution.filesToMigrate.isNotEmpty()) {
                val commitMessage = buildString {
                    append("Migrate diary data from previous repository")
                    append(" (${conflictResolution.filesToMigrate.size} files)")
                    if (conflictResolution.conflictsResolved.isNotEmpty()) {
                        append(" - ${conflictResolution.conflictsResolved.size} conflicts resolved")
                    }
                }

                // コミットユーザー情報を取得
                val commitUserName = settingsManager.gitCommitUserName.first()
                val commitUserEmail = settingsManager.gitCommitUserEmail.first()

                val migrateCommitCommand = git?.commit()?.setMessage(commitMessage)
                if (commitUserName.isNotBlank() && commitUserEmail.isNotBlank()) {
                    val author = PersonIdent(commitUserName, commitUserEmail)
                    migrateCommitCommand?.setAuthor(author)
                    migrateCommitCommand?.setCommitter(author)
                }
                migrateCommitCommand?.call()

                // 5. プッシュ
                git?.push()?.setCredentialsProvider(credentialsProvider)?.call()
                Log.d(TAG, "Successfully migrated ${conflictResolution.filesToMigrate.size} diary files")
            }
            
            // 6. 競合情報をログに記録
            if (conflictResolution.conflictsResolved.isNotEmpty()) {
                Log.i(TAG, "Resolved conflicts for: ${conflictResolution.conflictsResolved.joinToString(", ")}")
            }
            
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Data migration failed", e)
            Result.failure(e)
        }
    }

    /**
     * 競合をチェックして解決
     */
    private suspend fun checkAndResolveConflicts(localFiles: List<DiaryFile>): ConflictResolution = withContext(Dispatchers.IO) {
        val filesToMigrate = mutableListOf<DiaryFile>()
        val conflictsResolved = mutableListOf<String>()
        
        localFiles.forEach { localFile ->
            val existingFile = File(repoDirectory, localFile.fileName)
            
            if (existingFile.exists()) {
                // 競合が発生
                val existingContent = existingFile.readText()
                
                if (existingContent.trim() != localFile.content.trim()) {
                    // 内容が異なる場合の競合解決
                    val resolvedContent = resolveContentConflict(
                        localFile.fileName,
                        existingContent,
                        localFile.content,
                        localFile.lastModified,
                        existingFile.lastModified()
                    )
                    
                    filesToMigrate.add(
                        DiaryFile(
                            fileName = localFile.fileName,
                            content = resolvedContent,
                            lastModified = System.currentTimeMillis()
                        )
                    )
                    conflictsResolved.add(localFile.fileName)
                    
                    Log.i(TAG, "Resolved conflict for ${localFile.fileName}")
                } else {
                    // 内容が同じ場合はスキップ
                    Log.d(TAG, "Skipping ${localFile.fileName} - identical content")
                }
            } else {
                // 競合なし、そのまま移行
                filesToMigrate.add(localFile)
            }
        }
        
        ConflictResolution(filesToMigrate, conflictsResolved)
    }

    /**
     * 内容の競合を解決
     */
    private fun resolveContentConflict(
        fileName: String,
        existingContent: String,
        newContent: String,
        newLastModified: Long,
        existingLastModified: Long
    ): String {
        // 戦略1: より新しいファイルを優先
        return if (newLastModified > existingLastModified) {
            Log.d(TAG, "Using newer content for $fileName (from migration source)")
            newContent
        } else {
            Log.d(TAG, "Keeping existing content for $fileName (newer in destination)")
            existingContent
        }
        
        // 将来的な拡張: マージ戦略
        // return mergeContents(existingContent, newContent)
    }

    /**
     * 競合解決結果
     */
    data class ConflictResolution(
        val filesToMigrate: List<DiaryFile>,
        val conflictsResolved: List<String>
    )

    /**
     * データを破棄して新しいリポジトリに切り替え
     */
    private suspend fun discardAndSwitch(
        newRemoteUrl: String,
        newUsername: String,
        newPassword: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Discarding local data and switching to new repository")
            
            // 既存のリポジトリを削除
            repoDirectory?.deleteRecursively()
            
            // 新しいリポジトリを初期化
            initRepository(newRemoteUrl, newUsername, newPassword)
            
        } catch (e: Exception) {
            Log.e(TAG, "Discard and switch failed", e)
            Result.failure(e)
        }
    }

    /**
     * 日記ファイルのデータクラス
     */
    data class DiaryFile(
        val fileName: String,
        val content: String,
        val lastModified: Long
    )
}