package net.chasmine.oneline.model

import kotlinx.datetime.LocalDate

// KMP対応のDiaryEntry
data class DiaryEntry(
    val date: LocalDate,
    val content: String,
    val lastModified: Long = 0L,
) {
    fun getFileName(): String = "${'$'}date.md"
    fun getDisplayDate(): String = "${'$'}{date.year}年${'$'}{date.monthNumber}月${'$'}{date.dayOfMonth}日"
}
