package net.chasmine.oneline.util

import net.chasmine.oneline.data.model.DiaryEntry
import java.time.LocalDate
import java.time.YearMonth

/**
 * 日記の統計情報を計算するユーティリティクラス
 */
object DiaryStatistics {

    /**
     * 現在のストリーク（連続日数）を計算
     */
    fun calculateCurrentStreak(entries: List<DiaryEntry>): Int {
        if (entries.isEmpty()) return 0

        val sortedDates = entries.map { it.date }.distinct().sortedDescending()
        val today = LocalDate.now()

        // 今日または昨日から始まっているかチェック
        if (sortedDates.first() != today && sortedDates.first() != today.minusDays(1)) {
            return 0
        }

        var streak = 0
        var expectedDate = if (sortedDates.first() == today) today else today.minusDays(1)

        for (date in sortedDates) {
            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    /**
     * 最長ストリーク（連続日数）を計算
     */
    fun calculateLongestStreak(entries: List<DiaryEntry>): Int {
        if (entries.isEmpty()) return 0

        val sortedDates = entries.map { it.date }.distinct().sorted()

        var longestStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] == sortedDates[i - 1].plusDays(1)) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }

        return longestStreak
    }

    /**
     * 指定月の投稿数を計算
     */
    fun calculateMonthlyCount(entries: List<DiaryEntry>, yearMonth: YearMonth): Int {
        return entries.count { entry ->
            YearMonth.from(entry.date) == yearMonth
        }
    }

    /**
     * 総投稿数を計算
     */
    fun calculateTotalCount(entries: List<DiaryEntry>): Int {
        return entries.size
    }
}
