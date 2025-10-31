package net.chasmine.oneline.util

import net.chasmine.oneline.data.model.DiaryEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.isoDayNumber

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
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // 今日または昨日から始まっているかチェック
        if (sortedDates.first() != today && sortedDates.first() != today.minus(DatePeriod(days = 1))) {
            return 0
        }

        var streak = 0
        var expectedDate = if (sortedDates.first() == today) today else today.minus(DatePeriod(days = 1))

        for (date in sortedDates) {
            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minus(DatePeriod(days = 1))
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
            if (sortedDates[i] == sortedDates[i - 1].plus(DatePeriod(days = 1))) {
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
    fun calculateMonthlyCount(entries: List<DiaryEntry>, year: Int, month: Int): Int {
        return entries.count { entry ->
            entry.date.year == year && entry.date.monthNumber == month
        }
    }

    /**
     * 総投稿数を計算
     */
    fun calculateTotalCount(entries: List<DiaryEntry>): Int {
        return entries.size
    }

    /**
     * 過去7日間の投稿パターンを取得
     * @return 過去7日分の投稿有無（古い日→新しい日の順）
     */
    fun getWeeklyPattern(entries: List<DiaryEntry>): List<Boolean> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val entryDates = entries.map { it.date }.toSet()

        return (6 downTo 0).map { daysAgo ->
            val date = today.minus(DatePeriod(days = daysAgo))
            entryDates.contains(date)
        }
    }

    /**
     * GitHubスタイルのコントリビューショングラフ用のデータを取得
     * @param entries 全日記エントリー
     * @param weeks 表示する週数（デフォルト20週）
     * @return 各日付の投稿情報（日付、文字数）
     */
    data class ContributionDay(
        val date: LocalDate,
        val characterCount: Int
    )

    fun getContributionData(entries: List<DiaryEntry>, weeks: Int = 20): List<List<ContributionDay?>> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // エントリーを日付→文字数のマップに変換
        val entryMap = entries.associate { it.date to it.content.length }

        // 今日の曜日を取得（月曜日=1, 日曜日=7）
        val todayDayOfWeek = today.dayOfWeek.isoDayNumber

        // 最も古い日付を計算（今週の月曜日から weeks 週分遡る）
        val mondayOfThisWeek = today.minus(DatePeriod(days = todayDayOfWeek - 1))
        val startDate = mondayOfThisWeek.minus(DatePeriod(days = (weeks - 1) * 7))

        // 週ごとのデータを作成
        val result = mutableListOf<List<ContributionDay?>>()

        for (weekOffset in 0 until weeks) {
            val weekStart = startDate.plus(DatePeriod(days = weekOffset * 7))
            val week = mutableListOf<ContributionDay?>()

            for (dayOffset in 0 until 7) {
                val date = weekStart.plus(DatePeriod(days = dayOffset))

                // 未来の日付は null
                if (date > today) {
                    week.add(null)
                } else {
                    val charCount = entryMap[date] ?: 0
                    week.add(ContributionDay(date, charCount))
                }
            }

            result.add(week)
        }

        return result
    }

    /**
     * 文字数に基づいて貢献度レベルを計算（0-4）
     * - 0: 投稿なし
     * - 1: 1-10文字
     * - 2: 11-20文字
     * - 3: 21-30文字
     * - 4: 31文字以上
     */
    fun getContributionLevel(characterCount: Int): Int {
        return when {
            characterCount == 0 -> 0
            characterCount <= 10 -> 1
            characterCount <= 20 -> 2
            characterCount <= 30 -> 3
            else -> 4
        }
    }
}
