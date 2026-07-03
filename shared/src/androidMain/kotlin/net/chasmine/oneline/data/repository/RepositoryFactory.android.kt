package net.chasmine.oneline.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import net.chasmine.oneline.data.git.GitAuth
import net.chasmine.oneline.data.git.GitOperations
import net.chasmine.oneline.data.local.LocalRepository
import net.chasmine.oneline.data.model.DiaryEntry
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.preferences.SettingsStorage
import net.chasmine.oneline.data.storage.FileStorage
import net.chasmine.oneline.util.DateUtils
import java.io.File

/**
 * Android用のリポジトリファクトリー実装
 *
 * ローカルオンリーモードとGit連携モードを切り替えて適切なリポジトリを使用します。
 * Git連携モードでは「ローカルファースト」で動作します:
 * - 保存/削除はローカルクローンへのコミットまでを同期的に行い、即座に完了を返す
 * - リモートへのpushはバックグラウンドで実行する（失敗時はpull(OURS)後に再試行）
 * - リモートの取り込みは syncRepository()（pull）で行う
 */
actual class RepositoryFactory private constructor(private val context: Context) {

    private val TAG = "RepositoryFactory"
    private val settingsStorage = SettingsStorage(context)
    private val settingsManager = SettingsManager.getInstance(settingsStorage)
    private val fileStorage = FileStorage(context)
    private val localRepository = LocalRepository(fileStorage)
    private val gitOperations = GitOperations()

    // Git操作の直列化（バックグラウンドpushと同期・保存の競合防止）
    private val gitMutex = Mutex()

    // ViewModelが破棄されてもpushを完走させるためのアプリケーションスコープ
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repoDirectory: File
        get() = File(context.filesDir, GIT_REPO_DIR_NAME)

    private suspend fun isGitMode(): Boolean = !settingsManager.isLocalOnlyMode.first() &&
            settingsManager.gitRepoUrl.first().isNotBlank()

    private suspend fun commitUserName(): String = settingsManager.gitCommitUserName.first()

    private suspend fun commitUserEmail(): String = settingsManager.gitCommitUserEmail.first()

    actual suspend fun initialize(): Boolean {
        return try {
            // ローカルストレージは全モードで初期化（Gitモードでもフォールバック先として使用）
            val localReady = localRepository.initialize()
            if (!isGitMode()) {
                return localReady
            }

            val repoUrl = settingsManager.gitRepoUrl.first()
            val username = settingsManager.gitUsername.first()
            val token = settingsManager.gitToken.first()
            if (repoUrl.isBlank() || username.isBlank() || token.isBlank()) {
                Log.w(TAG, "Git settings not configured")
                return false
            }

            val result = gitMutex.withLock {
                gitOperations.initRepository(
                    repoUrl = repoUrl,
                    localPath = repoDirectory.absolutePath,
                    auth = GitAuth(username, token)
                )
            }

            if (result.isSuccess) {
                healLocalOnlyEntries()
                true
            } else {
                Log.e(TAG, "Git repository initialization failed", result.exceptionOrNull())
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize repository", e)
            false
        }
    }

    /**
     * ローカル側にのみ存在する日記をGitリポジトリへ回収する（自己修復）
     *
     * 過去バージョンではGit連携モードでもローカルストレージにのみ保存されていたため、
     * Gitに存在しない日付のエントリーが残っていればコミットして取り込む。
     */
    private suspend fun healLocalOnlyEntries() {
        try {
            val localEntries = localRepository.getAllEntries().first()
            if (localEntries.isEmpty()) return

            var migrated = 0
            gitMutex.withLock {
                localEntries.forEach { entry ->
                    val gitFile = File(repoDirectory, entry.getFileName())
                    if (!gitFile.exists()) {
                        val result = gitOperations.saveAndCommit(
                            fileName = entry.getFileName(),
                            content = entry.content,
                            commitMessage = "Add entry for ${entry.date}",
                            authorName = commitUserName(),
                            authorEmail = commitUserEmail()
                        )
                        if (result.isSuccess) migrated++
                    }
                }
            }
            if (migrated > 0) {
                Log.i(TAG, "Recovered $migrated local-only entries into Git repository")
                pushInBackground()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to heal local-only entries", e)
        }
    }

    actual suspend fun saveEntry(entry: DiaryEntry): Boolean {
        return try {
            if (!isGitMode()) {
                return localRepository.saveEntry(entry)
            }

            val committed = gitMutex.withLock {
                gitOperations.saveAndCommit(
                    fileName = entry.getFileName(),
                    content = entry.content,
                    commitMessage = "Update entry for ${entry.date}",
                    authorName = commitUserName(),
                    authorEmail = commitUserEmail()
                )
            }

            if (committed.isSuccess) {
                // 旧バージョンがローカル側に残した同日付のデータを掃除して二重管理を防ぐ
                localRepository.deleteEntry(entry.date.toString())
                pushInBackground()
                true
            } else {
                // Gitへのコミットに失敗しても日記を失わないようローカルへ退避保存する
                // （次回のinitialize時にhealLocalOnlyEntriesがGitへ回収する）
                Log.e(TAG, "Git commit failed, falling back to local save", committed.exceptionOrNull())
                localRepository.saveEntry(entry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save entry", e)
            false
        }
    }

    actual suspend fun getEntry(date: String): DiaryEntry? {
        return try {
            if (!isGitMode()) {
                return localRepository.getEntry(date)
            }

            val file = File(repoDirectory, "$date.md")
            if (file.exists() && file.canRead()) {
                withContext(Dispatchers.IO) {
                    DiaryEntry(
                        date = LocalDate.parse(date),
                        content = file.readText(),
                        lastModified = file.lastModified()
                    )
                }
            } else {
                // 旧バージョンでローカル側にのみ保存されたエントリーのフォールバック
                localRepository.getEntry(date)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get entry", e)
            null
        }
    }

    actual fun getAllEntries(): Flow<List<DiaryEntry>> = flow {
        try {
            if (!isGitMode()) {
                emit(localRepository.getAllEntries().first())
                return@flow
            }

            val gitEntries = readGitEntries()
            // 旧バージョンでローカル側にのみ残っているエントリーも表示から漏らさない
            val localOnlyEntries = localRepository.getAllEntries().first()
                .filter { local -> gitEntries.none { it.date == local.date } }

            emit((gitEntries + localOnlyEntries).sortedByDescending { it.date })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all entries", e)
            emit(emptyList())
        }
    }

    private suspend fun readGitEntries(): List<DiaryEntry> = withContext(Dispatchers.IO) {
        val dir = repoDirectory
        if (!dir.exists()) return@withContext emptyList()

        dir.listFiles { file ->
            file.isFile && file.name.endsWith(".md") &&
                    DateUtils.isValidDateFormat(file.nameWithoutExtension, "yyyy-MM-dd")
        }?.mapNotNull { file ->
            try {
                DiaryEntry(
                    date = LocalDate.parse(file.nameWithoutExtension),
                    content = file.readText(),
                    lastModified = file.lastModified()
                )
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    actual suspend fun deleteEntry(date: String): Boolean {
        return try {
            if (!isGitMode()) {
                return localRepository.deleteEntry(date)
            }

            val gitDeleted = gitMutex.withLock {
                gitOperations.deleteAndCommit(
                    fileName = "$date.md",
                    commitMessage = "Delete entry for $date",
                    authorName = commitUserName(),
                    authorEmail = commitUserEmail()
                )
            }
            // 旧バージョンのローカル側データも掃除（自己修復による復活を防ぐ）
            val localDeleted = localRepository.deleteEntry(date)

            if (gitDeleted.isSuccess) {
                pushInBackground()
            }
            gitDeleted.isSuccess || localDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete entry", e)
            false
        }
    }

    actual suspend fun syncRepository(): Boolean {
        return try {
            if (!isGitMode()) {
                return true
            }

            gitMutex.withLock {
                val pullResult = gitOperations.pull(useOursStrategy = true)
                if (pullResult.isFailure) {
                    Log.e(TAG, "Pull failed during sync", pullResult.exceptionOrNull())
                    return@withLock false
                }

                cleanupConflictMarkers()

                // pushは同期の成否に含めない（オフラインでもpull結果は反映済み）
                val pushResult = gitOperations.push()
                if (pushResult.isFailure) {
                    Log.w(TAG, "Push failed during sync", pushResult.exceptionOrNull())
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync repository", e)
            false
        }
    }

    /**
     * 競合マーカーが残った日記ファイルからマーカー行のみ除去する（本文は保持）
     */
    private suspend fun cleanupConflictMarkers() {
        try {
            val dir = repoDirectory
            if (!dir.exists()) return

            dir.listFiles { file -> file.isFile && file.name.endsWith(".md") }?.forEach { file ->
                val content = file.readText()
                if (content.contains("<<<<<<<") && content.contains(">>>>>>>")) {
                    val cleaned = content.lines()
                        .filterNot { line ->
                            line.startsWith("<<<<<<<") ||
                                    line.startsWith(">>>>>>>") ||
                                    line == "======="
                        }
                        .joinToString("\n")
                    if (cleaned != content) {
                        gitOperations.saveAndCommit(
                            fileName = file.name,
                            content = cleaned,
                            commitMessage = "Remove conflict markers from ${file.name}",
                            authorName = commitUserName(),
                            authorEmail = commitUserEmail()
                        )
                        Log.i(TAG, "Removed conflict markers from ${file.name} (content preserved)")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean up conflict markers", e)
        }
    }

    /**
     * リモートへのpushをバックグラウンドで実行する
     * 失敗時はpull(OURS戦略)で取り込み後に一度だけ再試行する
     */
    private fun pushInBackground() {
        backgroundScope.launch {
            try {
                val pushed = gitMutex.withLock { gitOperations.push() }
                if (pushed.isFailure) {
                    Log.w(TAG, "Background push failed, retrying after pull", pushed.exceptionOrNull())
                    gitMutex.withLock {
                        gitOperations.pull(useOursStrategy = true)
                        val retried = gitOperations.push()
                        if (retried.isFailure) {
                            Log.w(TAG, "Background push retry failed", retried.exceptionOrNull())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background push failed", e)
            }
        }
    }

    actual suspend fun getCurrentMode(): RepositoryMode {
        return try {
            if (isGitMode()) RepositoryMode.Git else RepositoryMode.LocalOnly
        } catch (e: Exception) {
            RepositoryMode.LocalOnly
        }
    }

    actual suspend fun hasValidSettings(): Boolean {
        return try {
            settingsManager.hasValidSettings.first()
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun migrateToGitMode(): MigrationResult {
        return try {
            val repoUrl = settingsManager.gitRepoUrl.first()
            val username = settingsManager.gitUsername.first()
            val token = settingsManager.gitToken.first()
            if (repoUrl.isBlank() || username.isBlank() || token.isBlank()) {
                return MigrationResult.GitSettingsNotConfigured
            }

            val initResult = gitMutex.withLock {
                gitOperations.initRepository(
                    repoUrl = repoUrl,
                    localPath = repoDirectory.absolutePath,
                    auth = GitAuth(username, token)
                )
            }
            if (initResult.isFailure) {
                return MigrationResult.GitInitializationFailed
            }

            val localEntries = localRepository.getAllEntries().first()
            var successCount = 0
            gitMutex.withLock {
                localEntries.forEach { entry ->
                    val result = gitOperations.saveAndCommit(
                        fileName = entry.getFileName(),
                        content = entry.content,
                        commitMessage = "Migrate entry for ${entry.date}",
                        authorName = commitUserName(),
                        authorEmail = commitUserEmail()
                    )
                    if (result.isSuccess) successCount++
                }
                gitOperations.push()
            }

            if (successCount < localEntries.size) {
                MigrationResult.DataMigrationFailed
            } else {
                MigrationResult.Success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate to Git mode", e)
            MigrationResult.UnknownError(e.message ?: "Unknown error")
        }
    }

    actual suspend fun migrateToLocalMode(clearGitData: Boolean): MigrationResult {
        return try {
            if (!localRepository.initialize()) {
                return MigrationResult.LocalInitializationFailed
            }

            readGitEntries().forEach { entry ->
                if (!localRepository.saveEntry(entry)) {
                    Log.w(TAG, "Failed to migrate entry to local: ${entry.date}")
                }
            }

            settingsManager.setLocalOnlyMode(true)
            MigrationResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate to local mode", e)
            MigrationResult.UnknownError(e.message ?: "Unknown error")
        }
    }

    actual companion object {
        // androidApp側のGitRepositoryと同じクローン先を共有する
        private const val GIT_REPO_DIR_NAME = "OneLine_repository"

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
