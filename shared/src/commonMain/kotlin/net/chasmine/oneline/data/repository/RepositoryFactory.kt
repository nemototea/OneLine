package net.chasmine.oneline.data.repository

import kotlinx.coroutines.flow.Flow
import net.chasmine.oneline.data.model.DiaryEntry

/**
 * プラットフォーム固有のリポジトリ管理を提供するファクトリー
 * expect/actualパターンで実装される
 */
expect class RepositoryFactory {
    /**
     * 現在のモードに応じたリポジトリを初期化
     */
    suspend fun initialize(): Boolean

    /**
     * 日記エントリーを保存
     */
    suspend fun saveEntry(entry: DiaryEntry): Boolean

    /**
     * 特定の日付の日記エントリーを取得
     */
    suspend fun getEntry(date: String): DiaryEntry?

    /**
     * すべての日記エントリーを取得
     */
    fun getAllEntries(): Flow<List<DiaryEntry>>

    /**
     * 日記エントリーを削除
     */
    suspend fun deleteEntry(date: String): Boolean

    /**
     * 同期処理（Git連携モードのみ）
     */
    suspend fun syncRepository(): Boolean

    /**
     * 現在のモードを取得
     */
    suspend fun getCurrentMode(): RepositoryMode

    /**
     * 設定が有効かどうかをチェック
     */
    suspend fun hasValidSettings(): Boolean

    /**
     * ローカルモードからGitモードへの移行
     */
    suspend fun migrateToGitMode(): MigrationResult

    /**
     * Gitモードからローカルモードへの移行
     */
    suspend fun migrateToLocalMode(clearGitData: Boolean): MigrationResult

    companion object {
        fun create(): RepositoryFactory
    }
}
