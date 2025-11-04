package net.chasmine.oneline.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Android向けファイルストレージ実装
 */
actual class FileStorage(private val context: Context) {

    /**
     * ファイルに内容を書き込む
     */
    actual suspend fun writeFile(path: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val file = resolveFile(path)

                // 親ディレクトリが存在しない場合は作成
                file.parentFile?.let { parent ->
                    if (!parent.exists()) {
                        parent.mkdirs()
                    }
                }

                file.writeText(content)
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ファイルの内容を読み込む
     */
    actual suspend fun readFile(path: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val file = resolveFile(path)

                if (!file.exists()) {
                    return@withContext Result.failure(
                        IOException("File does not exist: $path")
                    )
                }

                val content = file.readText()
                Result.success(content)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ファイルを削除する
     */
    actual suspend fun deleteFile(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val file = resolveFile(path)

                if (!file.exists()) {
                    return@withContext Result.success(Unit)
                }

                val deleted = file.delete()
                if (deleted) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("Failed to delete file: $path"))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ディレクトリ内のファイル一覧を取得
     */
    actual suspend fun listFiles(directory: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val dir = resolveFile(directory)

                if (!dir.exists()) {
                    return@withContext Result.success(emptyList())
                }

                if (!dir.isDirectory) {
                    return@withContext Result.failure(
                        IOException("Path is not a directory: $directory")
                    )
                }

                val files = dir.listFiles()?.map { it.name } ?: emptyList()
                Result.success(files)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ファイルが存在するか確認
     */
    actual suspend fun exists(path: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val file = resolveFile(path)
                Result.success(file.exists())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ディレクトリを作成
     */
    actual suspend fun createDirectory(directory: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val dir = resolveFile(directory)

                if (dir.exists()) {
                    if (dir.isDirectory) {
                        return@withContext Result.success(Unit)
                    } else {
                        return@withContext Result.failure(
                            IOException("Path exists but is not a directory: $directory")
                        )
                    }
                }

                val created = dir.mkdirs()
                if (created) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("Failed to create directory: $directory"))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * アプリ専用のデータディレクトリのパスを取得
     */
    actual fun getAppDirectory(): String {
        return context.filesDir.absolutePath
    }

    /**
     * パスを解決する（相対パスの場合はアプリディレクトリからの相対パスとして扱う）
     */
    private fun resolveFile(path: String): File {
        return if (path.startsWith("/")) {
            // 絶対パス
            File(path)
        } else {
            // 相対パス: アプリディレクトリからの相対パス
            File(context.filesDir, path)
        }
    }
}
