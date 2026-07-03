package net.chasmine.oneline.data.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.io.IOException

/**
 * Android向けGit操作実装（JGitベース）
 */
actual class GitOperations {
    private var git: Git? = null
    private var credentialsProvider: UsernamePasswordCredentialsProvider? = null
    private var localPath: String? = null
    private var isInitialized: Boolean = false

    /**
     * リポジトリ削除前に日記ファイル(YYYY-MM-DD.md)を退避する
     * リモートURL変更時などの deleteRecursively で未プッシュの日記が失われるのを防ぐ
     */
    private fun backupDiaryFilesBeforeDelete(repoDirectory: File) {
        try {
            if (!repoDirectory.exists()) return
            val mdFiles = repoDirectory.listFiles { file ->
                file.isFile && file.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.md"))
            } ?: return
            if (mdFiles.isEmpty()) return

            val backupRoot = File(repoDirectory.parentFile, "${repoDirectory.name}_backup")
            val backupDir = File(backupRoot, System.currentTimeMillis().toString())
            backupDir.mkdirs()
            mdFiles.forEach { it.copyTo(File(backupDir, it.name), overwrite = true) }

            // 古い退避データは直近5世代のみ保持
            backupRoot.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedByDescending { it.name.toLongOrNull() ?: 0L }
                ?.drop(5)
                ?.forEach { it.deleteRecursively() }
        } catch (_: Exception) {
            // 退避失敗は削除処理自体を妨げない
        }
    }

    actual suspend fun initRepository(
        repoUrl: String,
        localPath: String,
        auth: GitAuth
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            this@GitOperations.localPath = localPath
            credentialsProvider = UsernamePasswordCredentialsProvider(auth.username, auth.token)

            val repoDirectory = File(localPath)

            // 既に初期化済みかつ有効な場合はスキップ
            if (isInitialized && git != null && repoDirectory.exists()) {
                return@withContext Result.success(true)
            }

            // 既存のリポジトリがある場合、リモートURLを確認
            if (repoDirectory.exists() && File(repoDirectory, ".git").exists()) {
                try {
                    val existingGit = Git.open(repoDirectory)
                    val existingRemoteUrl = existingGit.repository.config.getString("remote", "origin", "url")
                    existingGit.close()

                    if (existingRemoteUrl != null && existingRemoteUrl != repoUrl) {
                        // URLが異なる場合は日記を退避してから既存のリポジトリを削除
                        backupDiaryFilesBeforeDelete(repoDirectory)
                        repoDirectory.deleteRecursively()
                    }
                } catch (e: Exception) {
                    // 既存のリポジトリを開けない場合は日記を退避してから削除
                    backupDiaryFilesBeforeDelete(repoDirectory)
                    repoDirectory.deleteRecursively()
                }
            }

            if (!repoDirectory.exists() || !File(repoDirectory, ".git").exists()) {
                // 新規クローン
                if (repoDirectory.exists()) {
                    backupDiaryFilesBeforeDelete(repoDirectory)
                    repoDirectory.deleteRecursively()
                }
                repoDirectory.mkdirs()

                git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDirectory)
                    .setCredentialsProvider(credentialsProvider)
                    .call()
            } else {
                // 既存のリポジトリを開く
                git = Git.open(repoDirectory)

                // 認証情報を更新
                git?.repository?.config?.apply {
                    setString("remote", "origin", "url", repoUrl)
                    save()
                }
            }

            isInitialized = true
            Result.success(true)
        } catch (e: GitAPIException) {
            isInitialized = false
            git = null
            Result.failure(e)
        } catch (e: IOException) {
            isInitialized = false
            git = null
            Result.failure(e)
        } catch (e: Exception) {
            isInitialized = false
            git = null
            Result.failure(e)
        }
    }

    actual suspend fun saveAndCommit(
        fileName: String,
        content: String,
        commitMessage: String,
        authorName: String,
        authorEmail: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null || localPath == null) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            val file = File(localPath, fileName)
            val repoDirectory = File(localPath!!)

            // ディレクトリが存在することを確認
            if (!repoDirectory.exists()) {
                repoDirectory.mkdirs()
            }

            // ファイルに内容を書き込み
            file.writeText(content)

            // Gitに変更を追加
            git?.add()?.addFilepattern(fileName)?.call()

            // コミット
            val commitCommand = git?.commit()?.setMessage(commitMessage)
            if (authorName.isNotBlank() && authorEmail.isNotBlank()) {
                val author = PersonIdent(authorName, authorEmail)
                commitCommand?.setAuthor(author)
                commitCommand?.setCommitter(author)
            }
            commitCommand?.call()

            Result.success(true)
        } catch (e: GitAPIException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun deleteAndCommit(
        fileName: String,
        commitMessage: String,
        authorName: String,
        authorEmail: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null || localPath == null) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            val file = File(localPath, fileName)

            if (file.exists()) {
                file.delete()

                // Gitから削除をステージング
                git?.rm()?.addFilepattern(fileName)?.call()

                // コミット
                val commitCommand = git?.commit()?.setMessage(commitMessage)
                if (authorName.isNotBlank() && authorEmail.isNotBlank()) {
                    val author = PersonIdent(authorName, authorEmail)
                    commitCommand?.setAuthor(author)
                    commitCommand?.setCommitter(author)
                }
                commitCommand?.call()

                return@withContext Result.success(true)
            }

            Result.failure(Exception("File does not exist"))
        } catch (e: GitAPIException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun push(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null || credentialsProvider == null) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            git?.push()?.setCredentialsProvider(credentialsProvider)?.call()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun pull(useOursStrategy: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null || credentialsProvider == null) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            // MERGING状態なら強制リセット
            val repo = git!!.repository
            if (repo.repositoryState.toString().contains("MERGING")) {
                // reset --hard で未コミットの日記内容が失われないよう退避する
                val status = git!!.status().call()
                val dirtyDiaryContents = (status.modified + status.untracked + status.conflicting)
                    .filter { it.endsWith(".md") }
                    .mapNotNull { name ->
                        val file = File(localPath, name)
                        if (file.exists()) name to file.readText() else null
                    }
                    .toMap()

                git!!.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call()

                // 退避した内容を復元してコミット
                var restoredAny = false
                dirtyDiaryContents.forEach { (name, content) ->
                    val file = File(localPath, name)
                    if (!file.exists() || file.readText() != content) {
                        file.writeText(content)
                        git!!.add().addFilepattern(name).call()
                        restoredAny = true
                    }
                }
                if (restoredAny) {
                    git!!.commit().setMessage("Recover local diary changes after merge abort").call()
                }
            }

            // pull実行
            val pullCommand = git!!.pull().setCredentialsProvider(credentialsProvider)
            if (useOursStrategy) {
                pullCommand.setStrategy(MergeStrategy.OURS)
            }
            pullCommand.call()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun addFile(filePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            git?.add()?.addFilepattern(filePath)?.call()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun hasUncommittedChanges(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            val hasChanges = git?.status()?.call()?.hasUncommittedChanges() ?: false
            Result.success(hasChanges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun isInitialized(): Boolean = isInitialized

    actual fun getLocalPath(): String? = localPath

    actual suspend fun getRemoteUrl(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || git == null) {
                return@withContext Result.failure(Exception("Repository not initialized"))
            }

            val remoteUrl = git?.repository?.config?.getString("remote", "origin", "url")
            Result.success(remoteUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
