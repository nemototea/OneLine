package net.chasmine.oneline

import net.chasmine.oneline.data.model.DiaryEntry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * シンプルで実用的なテスト
 * 
 * AIアシスタントが簡単に実行できる基本的なテストを提供します。
 * 複雑なMockingや依存性注入は使用せず、純粋な機能テストに焦点を当てます。
 */
class SimpleTest {

    @Test
    fun `DiaryEntry - 基本的な作成と取得が正常に動作すること`() {
        // Given
        val testDate = LocalDate.of(2025, 8, 3)
        val testContent = "今日は良い天気でした。"

        // When
        val entry = DiaryEntry(date = testDate, content = testContent)

        // Then
        assertEquals("日付が正しく設定されること", testDate, entry.date)
        assertEquals("内容が正しく設定されること", testContent, entry.content)
    }

    @Test
    fun `DiaryEntry - 空の内容でも作成できること`() {
        // Given
        val testDate = LocalDate.of(2025, 8, 3)
        val emptyContent = ""

        // When
        val entry = DiaryEntry(date = testDate, content = emptyContent)

        // Then
        assertEquals("日付が正しく設定されること", testDate, entry.date)
        assertEquals("空の内容でも設定されること", emptyContent, entry.content)
    }

    @Test
    fun `DiaryEntry - 長い内容でも作成できること`() {
        // Given
        val testDate = LocalDate.of(2025, 8, 3)
        val longContent = "今日は".repeat(1000) // 3000文字の長い内容

        // When
        val entry = DiaryEntry(date = testDate, content = longContent)

        // Then
        assertEquals("日付が正しく設定されること", testDate, entry.date)
        assertEquals("長い内容でも設定されること", longContent, entry.content)
        assertTrue("内容の長さが正しいこと", entry.content.length == 3000)
    }

    @Test
    fun `DiaryEntry - 複数のエントリーを作成して比較できること`() {
        // Given
        val date1 = LocalDate.of(2025, 8, 1)
        val date2 = LocalDate.of(2025, 8, 2)
        val date3 = LocalDate.of(2025, 8, 3)

        // When
        val entry1 = DiaryEntry(date1, "1日目の日記")
        val entry2 = DiaryEntry(date2, "2日目の日記")
        val entry3 = DiaryEntry(date3, "3日目の日記")

        val entries = listOf(entry1, entry2, entry3)

        // Then
        assertEquals("エントリー数が正しいこと", 3, entries.size)
        assertEquals("1番目のエントリーの日付", date1, entries[0].date)
        assertEquals("2番目のエントリーの日付", date2, entries[1].date)
        assertEquals("3番目のエントリーの日付", date3, entries[2].date)
    }

    @Test
    fun `DiaryEntry - 日付でソートできること`() {
        // Given
        val entries = listOf(
            DiaryEntry(LocalDate.of(2025, 8, 3), "3日目"),
            DiaryEntry(LocalDate.of(2025, 8, 1), "1日目"),
            DiaryEntry(LocalDate.of(2025, 8, 2), "2日目")
        )

        // When
        val sortedEntries = entries.sortedByDescending { it.date }

        // Then
        assertEquals("ソート後の1番目", LocalDate.of(2025, 8, 3), sortedEntries[0].date)
        assertEquals("ソート後の2番目", LocalDate.of(2025, 8, 2), sortedEntries[1].date)
        assertEquals("ソート後の3番目", LocalDate.of(2025, 8, 1), sortedEntries[2].date)
    }

    @Test
    fun `DiaryEntry - 今日の日記を識別できること`() {
        // Given
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        val entries = listOf(
            DiaryEntry(yesterday, "昨日の日記"),
            DiaryEntry(today, "今日の日記"),
            DiaryEntry(tomorrow, "明日の日記")
        )

        // When
        val todayEntry = entries.find { it.date == today }

        // Then
        assertNotNull("今日の日記が見つかること", todayEntry)
        assertEquals("今日の日記の内容が正しいこと", "今日の日記", todayEntry?.content)
    }

    @Test
    fun `文字列処理 - 日付フォーマットが正しく動作すること`() {
        // Given
        val testDate = LocalDate.of(2025, 8, 3)

        // When
        val dateString = testDate.toString()

        // Then
        assertEquals("日付文字列が正しいフォーマットであること", "2025-08-03", dateString)
    }

    @Test
    fun `文字列処理 - 内容のトリムが正しく動作すること`() {
        // Given
        val contentWithSpaces = "  今日の日記  "

        // When
        val trimmedContent = contentWithSpaces.trim()

        // Then
        assertEquals("前後の空白が削除されること", "今日の日記", trimmedContent)
    }

    @Test
    fun `リスト操作 - フィルタリングが正しく動作すること`() {
        // Given
        val entries = listOf(
            DiaryEntry(LocalDate.of(2025, 8, 1), ""),
            DiaryEntry(LocalDate.of(2025, 8, 2), "内容あり"),
            DiaryEntry(LocalDate.of(2025, 8, 3), ""),
            DiaryEntry(LocalDate.of(2025, 8, 4), "もう一つの内容")
        )

        // When
        val nonEmptyEntries = entries.filter { it.content.isNotBlank() }

        // Then
        assertEquals("空でないエントリーが2つあること", 2, nonEmptyEntries.size)
        assertEquals("1番目の内容", "内容あり", nonEmptyEntries[0].content)
        assertEquals("2番目の内容", "もう一つの内容", nonEmptyEntries[1].content)
    }

    @Test
    fun `パフォーマンス - 大量のエントリー作成が妥当な時間で完了すること`() {
        // Given
        val entryCount = 1000

        // When
        val startTime = System.currentTimeMillis()
        val entries = (1..entryCount).map { i ->
            DiaryEntry(
                LocalDate.of(2025, 1, 1).plusDays(i.toLong()),
                "日記 $i"
            )
        }
        val endTime = System.currentTimeMillis()

        // Then
        assertEquals("指定した数のエントリーが作成されること", entryCount, entries.size)
        assertTrue("処理時間が妥当であること（1秒以内）", (endTime - startTime) < 1000)
    }
}
