package net.chasmine.oneline.data.repository

import kotlinx.coroutines.flow.Flow
import net.chasmine.oneline.data.model.DiaryEntry

/**
 * 日記リポジトリの共通インターフェース
 * ローカルストレージとGitリポジトリの両方で実装される
 */
interface DiaryRepository {
    /**
     * 日記エントリーを保存
     * @param entry 保存する日記エントリー
     * @return 成功した場合true
     */
    suspend fun saveEntry(entry: DiaryEntry): Boolean

    /**
     * 特定の日付の日記エントリーを取得
     * @param date 日付文字列（yyyy-MM-dd形式）
     * @return 日記エントリー、存在しない場合null
     */
    suspend fun getEntry(date: String): DiaryEntry?

    /**
     * すべての日記エントリーを取得
     * @return 日記エントリーのリストを返すFlow
     */
    fun getAllEntries(): Flow<List<DiaryEntry>>

    /**
     * 日記エントリーを削除
     * @param date 日付文字列（yyyy-MM-dd形式）
     * @return 成功した場合true
     */
    suspend fun deleteEntry(date: String): Boolean
}
