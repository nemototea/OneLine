package net.chasmine.oneline.data.model

import java.time.LocalDate

data class DiaryEntry(
    val date: LocalDate,
    val content: String,
    val lastModified: Long = System.currentTimeMillis(),
) {
    // ファイル名としてYYYY-MM-DD.mdの形式で取得
    fun getFileName(): String {
        return "${date}.md"
    }

    // 表示用の日付 (日本語形式)
    fun getDisplayDate(): String {
        return "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }
}