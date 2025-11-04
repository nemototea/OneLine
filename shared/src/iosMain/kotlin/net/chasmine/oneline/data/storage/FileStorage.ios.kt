package net.chasmine.oneline.data.storage

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.posix.memcpy

/**
 * iOS向けファイルストレージ実装
 */
@OptIn(ExperimentalForeignApi::class)
actual class FileStorage {

    private val fileManager = NSFileManager.defaultManager

    /**
     * ファイルに内容を書き込む
     */
    actual suspend fun writeFile(path: String, content: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                val filePath = resolvePath(path)
                val nsString = content as NSString

                // 親ディレクトリが存在しない場合は作成
                val parentPath = (filePath as NSString).stringByDeletingLastPathComponent
                if (!fileManager.fileExistsAtPath(parentPath)) {
                    memScoped {
                        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                        val created = fileManager.createDirectoryAtPath(
                            path = parentPath,
                            withIntermediateDirectories = true,
                            attributes = null,
                            error = errorPtr.ptr
                        )
                        if (!created) {
                            val error = errorPtr.value
                            return@withContext Result.failure(
                                Exception(error?.localizedDescription ?: "Failed to create directory")
                            )
                        }
                    }
                }

                // ファイルに書き込み
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val written = nsString.writeToFile(
                        path = filePath,
                        atomically = true,
                        encoding = NSUTF8StringEncoding,
                        error = errorPtr.ptr
                    )
                    if (written) {
                        Result.success(Unit)
                    } else {
                        val error = errorPtr.value
                        Result.failure(
                            Exception(error?.localizedDescription ?: "Failed to write file")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ファイルの内容を読み込む
     */
    actual suspend fun readFile(path: String): Result<String> =
        withContext(Dispatchers.Default) {
            try {
                val filePath = resolvePath(path)

                if (!fileManager.fileExistsAtPath(filePath)) {
                    return@withContext Result.failure(
                        Exception("File does not exist: $path")
                    )
                }

                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val content = NSString.stringWithContentsOfFile(
                        path = filePath,
                        encoding = NSUTF8StringEncoding,
                        error = errorPtr.ptr
                    )

                    if (content != null) {
                        Result.success(content as String)
                    } else {
                        val error = errorPtr.value
                        Result.failure(
                            Exception(error?.localizedDescription ?: "Failed to read file")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ファイルを削除する
     */
    actual suspend fun deleteFile(path: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                val filePath = resolvePath(path)

                if (!fileManager.fileExistsAtPath(filePath)) {
                    return@withContext Result.success(Unit)
                }

                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val deleted = fileManager.removeItemAtPath(
                        path = filePath,
                        error = errorPtr.ptr
                    )

                    if (deleted) {
                        Result.success(Unit)
                    } else {
                        val error = errorPtr.value
                        Result.failure(
                            Exception(error?.localizedDescription ?: "Failed to delete file")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ディレクトリ内のファイル一覧を取得
     */
    actual suspend fun listFiles(directory: String): Result<List<String>> =
        withContext(Dispatchers.Default) {
            try {
                val dirPath = resolvePath(directory)

                if (!fileManager.fileExistsAtPath(dirPath)) {
                    return@withContext Result.success(emptyList())
                }

                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val contents = fileManager.contentsOfDirectoryAtPath(
                        path = dirPath,
                        error = errorPtr.ptr
                    )

                    if (contents != null) {
                        @Suppress("UNCHECKED_CAST")
                        val fileList = (contents as List<*>).mapNotNull { it as? String }
                        Result.success(fileList)
                    } else {
                        val error = errorPtr.value
                        Result.failure(
                            Exception(error?.localizedDescription ?: "Failed to list files")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ファイルが存在するか確認
     */
    actual suspend fun exists(path: String): Result<Boolean> =
        withContext(Dispatchers.Default) {
            try {
                val filePath = resolvePath(path)
                val exists = fileManager.fileExistsAtPath(filePath)
                Result.success(exists)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ディレクトリを作成
     */
    actual suspend fun createDirectory(directory: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                val dirPath = resolvePath(directory)

                if (fileManager.fileExistsAtPath(dirPath)) {
                    return@withContext Result.success(Unit)
                }

                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val created = fileManager.createDirectoryAtPath(
                        path = dirPath,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = errorPtr.ptr
                    )

                    if (created) {
                        Result.success(Unit)
                    } else {
                        val error = errorPtr.value
                        Result.failure(
                            Exception(error?.localizedDescription ?: "Failed to create directory")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * アプリ専用のデータディレクトリのパスを取得
     */
    actual fun getAppDirectory(): String {
        val urls = fileManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        )
        val documentDirectory = urls.firstOrNull() as? NSURL
        return documentDirectory?.path ?: ""
    }

    /**
     * パスを解決する（相対パスの場合はアプリディレクトリからの相対パスとして扱う）
     */
    private fun resolvePath(path: String): String {
        return if (path.startsWith("/")) {
            // 絶対パス
            path
        } else {
            // 相対パス: アプリディレクトリからの相対パス
            "${getAppDirectory()}/$path"
        }
    }
}
