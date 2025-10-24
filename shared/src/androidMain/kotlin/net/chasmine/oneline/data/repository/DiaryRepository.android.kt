package net.chasmine.oneline.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import net.chasmine.oneline.data.model.DiaryEntry

/**
 * Android実装: DiaryRepositoryFactory
 * Android固有のRepositoryManagerを使用
 */
actual class DiaryRepositoryFactory(private val context: Context) {
    actual fun createRepository(): DiaryRepository {
        // Android固有のリポジトリ実装を返す
        // 現状はLocalRepositoryをラップする
        return AndroidDiaryRepository(context)
    }
}

/**
 * Android専用のリポジトリ実装
 * 既存のRepositoryManagerをラップ
 */
class AndroidDiaryRepository(private val context: Context) : DiaryRepository {
    // TODO: 既存のRepositoryManagerを統合
    
    override suspend fun saveEntry(entry: DiaryEntry): Boolean {
        // 実装は後で既存コードから移行
        return false
    }
    
    override suspend fun getEntry(date: LocalDate): DiaryEntry? {
        // 実装は後で既存コードから移行
        return null
    }
    
    override fun getAllEntries(): Flow<List<DiaryEntry>> {
        // 実装は後で既存コードから移行
        return flow { emit(emptyList()) }
    }
    
    override suspend fun deleteEntry(date: LocalDate): Boolean {
        // 実装は後で既存コードから移行
        return false
    }
    
    override suspend fun initialize(): Boolean {
        // 実装は後で既存コードから移行
        return true
    }
}
