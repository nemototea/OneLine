package net.chasmine.oneline.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import net.chasmine.oneline.data.model.DiaryEntry

/**
 * 日記リポジトリのインターフェース
 * プラットフォーム固有の実装はexpectで定義
 */
interface DiaryRepository {
    /**
     * 日記エントリーを保存
     */
    suspend fun saveEntry(entry: DiaryEntry): Boolean
    
    /**
     * 指定日付の日記エントリーを取得
     */
    suspend fun getEntry(date: LocalDate): DiaryEntry?
    
    /**
     * すべての日記エントリーを取得（Flowで監視可能）
     */
    fun getAllEntries(): Flow<List<DiaryEntry>>
    
    /**
     * 日記エントリーを削除
     */
    suspend fun deleteEntry(date: LocalDate): Boolean
    
    /**
     * リポジトリの初期化
     */
    suspend fun initialize(): Boolean
}

/**
 * プラットフォーム固有のリポジトリファクトリ
 */
expect class DiaryRepositoryFactory {
    fun createRepository(): DiaryRepository
}
