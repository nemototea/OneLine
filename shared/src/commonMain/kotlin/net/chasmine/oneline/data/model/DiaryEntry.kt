package net.chasmine.oneline.data.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * 日記エントリのデータモデル（KMP対応版）
 *
 * kotlinx-datetimeを使用してマルチプラットフォーム対応
 */
@Serializable
data class DiaryEntry(
    val date: LocalDate,
    val content: String,
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
) {
    // ファイル名としてYYYY-MM-DD.mdの形式で取得
    fun getFileName(): String {
        return "${date}.md"
    }

    // 表示用の日付 (日本語形式)
    fun getDisplayDate(): String {
        return "${date.year}年${date.monthNumber}月${date.dayOfMonth}日"
    }
}
