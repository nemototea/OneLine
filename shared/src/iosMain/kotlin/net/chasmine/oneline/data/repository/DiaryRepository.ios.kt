package net.chasmine.oneline.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import net.chasmine.oneline.data.model.DiaryEntry
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

/**
 * iOS実装: DiaryRepositoryFactory
 */
actual class DiaryRepositoryFactory {
    actual fun createRepository(): DiaryRepository {
        return IosDiaryRepository()
    }
}

/**
 * iOS専用のリポジトリ実装
 * iOSのファイルシステムAPIを使用
 */
class IosDiaryRepository : DiaryRepository {
    private val fileManager = NSFileManager.defaultManager
    
    private fun getDocumentsDirectory(): String {
        val paths = fileManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        )
        return paths.firstOrNull()?.path ?: ""
    }
    
    override suspend fun saveEntry(entry: DiaryEntry): Boolean {
        // iOS固有のファイル保存実装
        // TODO: 実装
        return false
    }
    
    override suspend fun getEntry(date: LocalDate): DiaryEntry? {
        // iOS固有のファイル読み込み実装
        // TODO: 実装
        return null
    }
    
    override fun getAllEntries(): Flow<List<DiaryEntry>> {
        // iOS固有のファイル一覧取得実装
        // TODO: 実装
        return flow { emit(emptyList()) }
    }
    
    override suspend fun deleteEntry(date: LocalDate): Boolean {
        // iOS固有のファイル削除実装
        // TODO: 実装
        return false
    }
    
    override suspend fun initialize(): Boolean {
        // ディレクトリの作成など初期化処理
        // TODO: 実装
        return true
    }
}
