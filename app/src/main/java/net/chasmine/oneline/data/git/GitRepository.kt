package net.chasmine.oneline.data.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.PullResult
import java.io.File

import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.ssh.apache.DefaultSshSessionFactory

class GitRepository {
    /**
     * HTTPSでリポジトリをクローンする
     */
    suspend fun cloneRepository(
        remoteUrl: String,
        localPath: File,
        username: String,
        password: String
    ): Result<Git> = withContext(Dispatchers.IO) {
        try {
            val credentialsProvider = UsernamePasswordCredentialsProvider(username, password)

            val git = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(localPath)
                .setCredentialsProvider(credentialsProvider)
                .call()

            Result.success(git)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * SSH経由でリポジトリをクローンする
     */
    suspend fun cloneRepositoryWithSsh(
        remoteUrl: String,
        localPath: File,
        privateKeyPath: File,
        passphrase: String? = null
    ): Result<Git> = withContext(Dispatchers.IO) {
        try {
            // SSH設定
            val sshSessionFactory = object : DefaultSshSessionFactory() {
                override fun getDefaultKeysFile(): List<File> {
                    // デフォルトのSSHキーリストに追加
                    val keys = super.getDefaultKeysFile().toMutableList()
                    keys.add(privateKeyPath)
                    return keys
                }
            }

            // トランスポート設定
            val transportConfigCallback = TransportConfigCallback { transport ->
                if (transport is SshTransport) {
                    transport.sshSessionFactory = sshSessionFactory
                }
            }

            val git = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(localPath)
                .setTransportConfigCallback(transportConfigCallback)
                .call()

            Result.success(git)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * リモートからプル
     */
    suspend fun pullFromRemote(
        repositoryPath: File,
        username: String,
        password: String,
        remoteBranch: String = "origin/main"
    ): Result<PullResult> = withContext(Dispatchers.IO) {
        try {
            val credentialsProvider = UsernamePasswordCredentialsProvider(username, password)

            Git.open(repositoryPath).use { git ->
                val pullResult = git.pull()
                    .setCredentialsProvider(credentialsProvider)
                    .setRemoteBranchName(remoteBranch)
                    .call()

                Result.success(pullResult)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * リモートにプッシュ
     */
    suspend fun pushToRemote(
        repositoryPath: File,
        username: String,
        password: String,
        remoteName: String = "origin",
        pushAll: Boolean = false
    ): Result<Iterable<PushResult>> = withContext(Dispatchers.IO) {
        try {
            val credentialsProvider = UsernamePasswordCredentialsProvider(username, password)

            Git.open(repositoryPath).use { git ->
                val pushCommand = git.push()
                    .setCredentialsProvider(credentialsProvider)
                    .setRemote(remoteName)

                if (pushAll) {
                    pushCommand.setPushAll()
                }

                val result = pushCommand.call()
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}