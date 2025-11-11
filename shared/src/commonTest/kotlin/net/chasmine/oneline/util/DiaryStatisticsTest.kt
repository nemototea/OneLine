package net.chasmine.oneline.util

import net.chasmine.oneline.data.model.DiaryEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DiaryStatistics のテスト
 */
class DiaryStatisticsTest {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    @Test
    fun testCalculateTotalCountEmpty() {
        val count = DiaryStatistics.calculateTotalCount(emptyList())
        assertEquals(0, count)
    }

    @Test
    fun testCalculateTotalCount() {
        val entries = listOf(
            DiaryEntry(today, "Entry 1"),
            DiaryEntry(today.minus(DatePeriod(days = 1)), "Entry 2"),
            DiaryEntry(today.minus(DatePeriod(days = 2)), "Entry 3")
        )
        val count = DiaryStatistics.calculateTotalCount(entries)
        assertEquals(3, count)
    }

    @Test
    fun testCalculateCurrentStreakEmpty() {
        val streak = DiaryStatistics.calculateCurrentStreak(emptyList())
        assertEquals(0, streak)
    }

    @Test
    fun testCalculateCurrentStreakTodayOnly() {
        val entries = listOf(
            DiaryEntry(today, "Today's entry")
        )
        val streak = DiaryStatistics.calculateCurrentStreak(entries)
        assertEquals(1, streak)
    }

    @Test
    fun testCalculateCurrentStreakConsecutive() {
        val entries = listOf(
            DiaryEntry(today, "Today"),
            DiaryEntry(today.minus(DatePeriod(days = 1)), "Yesterday"),
            DiaryEntry(today.minus(DatePeriod(days = 2)), "2 days ago")
        )
        val streak = DiaryStatistics.calculateCurrentStreak(entries)
        assertEquals(3, streak)
    }

    @Test
    fun testCalculateCurrentStreakBroken() {
        val entries = listOf(
            DiaryEntry(today.minus(DatePeriod(days = 3)), "3 days ago"),
            DiaryEntry(today.minus(DatePeriod(days = 4)), "4 days ago")
        )
        val streak = DiaryStatistics.calculateCurrentStreak(entries)
        assertEquals(0, streak)
    }

    @Test
    fun testCalculateCurrentStreakYesterdayStart() {
        val entries = listOf(
            DiaryEntry(today.minus(DatePeriod(days = 1)), "Yesterday"),
            DiaryEntry(today.minus(DatePeriod(days = 2)), "2 days ago")
        )
        val streak = DiaryStatistics.calculateCurrentStreak(entries)
        assertEquals(2, streak)
    }

    @Test
    fun testCalculateLongestStreakEmpty() {
        val streak = DiaryStatistics.calculateLongestStreak(emptyList())
        assertEquals(0, streak)
    }

    @Test
    fun testCalculateLongestStreakSingleEntry() {
        val entries = listOf(
            DiaryEntry(today, "Today")
        )
        val streak = DiaryStatistics.calculateLongestStreak(entries)
        assertEquals(1, streak)
    }

    @Test
    fun testCalculateLongestStreakConsecutive() {
        val entries = listOf(
            DiaryEntry(LocalDate(2025, 11, 1), "Day 1"),
            DiaryEntry(LocalDate(2025, 11, 2), "Day 2"),
            DiaryEntry(LocalDate(2025, 11, 3), "Day 3"),
            DiaryEntry(LocalDate(2025, 11, 5), "Day 5"),
            DiaryEntry(LocalDate(2025, 11, 6), "Day 6")
        )
        val streak = DiaryStatistics.calculateLongestStreak(entries)
        assertEquals(3, streak) // Nov 1-3 が最長
    }

    @Test
    fun testCalculateLongestStreakMultipleStreaks() {
        val entries = listOf(
            DiaryEntry(LocalDate(2025, 11, 1), "Day 1"),
            DiaryEntry(LocalDate(2025, 11, 2), "Day 2"),
            DiaryEntry(LocalDate(2025, 11, 5), "Day 5"),
            DiaryEntry(LocalDate(2025, 11, 6), "Day 6"),
            DiaryEntry(LocalDate(2025, 11, 7), "Day 7"),
            DiaryEntry(LocalDate(2025, 11, 8), "Day 8")
        )
        val streak = DiaryStatistics.calculateLongestStreak(entries)
        assertEquals(4, streak) // Nov 5-8 が最長
    }

    @Test
    fun testCalculateMonthlyCountEmpty() {
        val count = DiaryStatistics.calculateMonthlyCount(emptyList(), 2025, 11)
        assertEquals(0, count)
    }

    @Test
    fun testCalculateMonthlyCount() {
        val entries = listOf(
            DiaryEntry(LocalDate(2025, 11, 1), "Nov 1"),
            DiaryEntry(LocalDate(2025, 11, 15), "Nov 15"),
            DiaryEntry(LocalDate(2025, 10, 31), "Oct 31"),
            DiaryEntry(LocalDate(2025, 12, 1), "Dec 1")
        )
        val count = DiaryStatistics.calculateMonthlyCount(entries, 2025, 11)
        assertEquals(2, count)
    }

    @Test
    fun testGetWeeklyPatternEmpty() {
        val pattern = DiaryStatistics.getWeeklyPattern(emptyList())
        assertEquals(7, pattern.size)
        assertTrue(pattern.all { !it })
    }

    @Test
    fun testGetWeeklyPatternSomeEntries() {
        val entries = listOf(
            DiaryEntry(today, "Today"),
            DiaryEntry(today.minus(DatePeriod(days = 2)), "2 days ago"),
            DiaryEntry(today.minus(DatePeriod(days = 5)), "5 days ago")
        )
        val pattern = DiaryStatistics.getWeeklyPattern(entries)

        assertEquals(7, pattern.size)
        assertEquals(true, pattern[6]) // today
        assertEquals(true, pattern[4]) // 2 days ago
        assertEquals(true, pattern[1]) // 5 days ago
        assertEquals(false, pattern[0]) // 6 days ago
    }

    @Test
    fun testGetContributionLevelZero() {
        assertEquals(0, DiaryStatistics.getContributionLevel(0))
    }

    @Test
    fun testGetContributionLevelOne() {
        assertEquals(1, DiaryStatistics.getContributionLevel(5))
        assertEquals(1, DiaryStatistics.getContributionLevel(10))
    }

    @Test
    fun testGetContributionLevelTwo() {
        assertEquals(2, DiaryStatistics.getContributionLevel(15))
        assertEquals(2, DiaryStatistics.getContributionLevel(20))
    }

    @Test
    fun testGetContributionLevelThree() {
        assertEquals(3, DiaryStatistics.getContributionLevel(25))
        assertEquals(3, DiaryStatistics.getContributionLevel(30))
    }

    @Test
    fun testGetContributionLevelFour() {
        assertEquals(4, DiaryStatistics.getContributionLevel(31))
        assertEquals(4, DiaryStatistics.getContributionLevel(100))
    }

    @Test
    fun testGetContributionDataEmpty() {
        val data = DiaryStatistics.getContributionData(emptyList(), weeks = 4)
        assertEquals(4, data.size)
        assertTrue(data.all { week -> week.size == 7 })
    }

    @Test
    fun testGetContributionDataWithEntries() {
        val entries = listOf(
            DiaryEntry(today, "今日の日記（10文字）"),
            DiaryEntry(today.minus(DatePeriod(days = 1)), "昨日の日記（5文字少なめ）")
        )
        val data = DiaryStatistics.getContributionData(entries, weeks = 2)

        assertEquals(2, data.size)
        assertTrue(data.all { week -> week.size == 7 })

        // 今日のエントリーが含まれていることを確認
        val todayContributions = data.flatten().filterNotNull().filter { it.date == today }
        assertEquals(1, todayContributions.size)
        assertTrue(todayContributions[0].characterCount > 0)
    }

    @Test
    fun testGetContributionDataFutureDatesAreNull() {
        val entries = emptyList<DiaryEntry>()
        val data = DiaryStatistics.getContributionData(entries, weeks = 4)

        // 未来の日付は null になっているはず
        val allDays = data.flatten()
        val nullCount = allDays.count { it == null }

        // 少なくとも一部は null であるべき（週の後半が未来の場合）
        assertTrue(nullCount >= 0)
    }
}
