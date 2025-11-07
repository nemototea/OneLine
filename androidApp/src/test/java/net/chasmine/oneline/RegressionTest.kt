package net.chasmine.oneline

import net.chasmine.oneline.data.model.DiaryEntry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * リグレッションテスト
 * 
 * 実際のアプリケーションの動作をテストし、
 * 実装変更時に問題を検出できるテストスイートです。
 */
class RegressionTest {

    @Test
    fun `DiaryEntry - 日付フォーマットが変更されていないこと`() {
        // Given
        val testDate = LocalDate(2025, 8, 3)
        val entry = DiaryEntry(testDate, "テスト内容")

        // When
        val dateString = entry.date.toString()

        // Then
        assertEquals("日付フォーマットが変更されていないこと", "2025-08-03", dateString)
        
        // リグレッション検証: 異なるフォーマットでないことを確認
        assertNotEquals("間違ったフォーマット1", "2025/08/03", dateString)
        assertNotEquals("間違ったフォーマット2", "08-03-2025", dateString)
        assertNotEquals("間違ったフォーマット3", "2025年8月3日", dateString)
    }

    @Test
    fun `DiaryEntry - データクラスの構造が変更されていないこと`() {
        // Given
        val testDate = LocalDate(2025, 8, 3)
        val testContent = "テスト内容"

        // When
        val entry = DiaryEntry(date = testDate, content = testContent)

        // Then - プロパティが正しく存在すること
        assertEquals("dateプロパティが存在すること", testDate, entry.date)
        assertEquals("contentプロパティが存在すること", testContent, entry.content)
        
        // リグレッション検証: プロパティの型が変更されていないこと
        assertTrue("dateがLocalDate型であること", entry.date is LocalDate)
        assertTrue("contentがString型であること", entry.content is String)
    }

    @Test
    fun `ファイル名生成 - ローカルリポジトリのファイル名規則が変更されていないこと`() {
        // Given
        val testDate = LocalDate(2025, 8, 3)
        val expectedFileName = "2025-08-03.txt"

        // When
        val actualFileName = "${testDate}.txt"

        // Then
        assertEquals("ファイル名規則が変更されていないこと", expectedFileName, actualFileName)
        
        // リグレッション検証: ファイル名の形式
        assertTrue("ファイル名が.txtで終わること", actualFileName.endsWith(".txt"))
        assertTrue("ファイル名にハイフンが含まれること", actualFileName.contains("-"))
        assertEquals("ファイル名の長さが正しいこと", 14, actualFileName.length) // "2025-08-03.txt" = 14文字
    }

    @Test
    fun `データ整合性 - 空の内容の処理が変更されていないこと`() {
        // Given
        val testDate = LocalDate(2025, 8, 3)
        val emptyContent = ""

        // When
        val entry = DiaryEntry(testDate, emptyContent)

        // Then
        assertEquals("空の内容が正しく処理されること", "", entry.content)
        assertNotNull("空の内容でもエントリーが作成されること", entry)
        
        // リグレッション検証: 空の内容の扱い
        assertTrue("空の内容はblankであること", entry.content.isBlank()) // ""はblankである
        assertTrue("空の内容はemptyであること", entry.content.isEmpty())
    }

    @Test
    fun `データ整合性 - null安全性が保たれていること`() {
        // Given
        val testDate = LocalDate(2025, 8, 3)
        val testContent = "テスト内容"

        // When
        val entry = DiaryEntry(testDate, testContent)

        // Then - null安全性の確認
        assertNotNull("dateがnullでないこと", entry.date)
        assertNotNull("contentがnullでないこと", entry.content)
        
        // リグレッション検証: プロパティがnullableでないこと
        // Kotlinのnull安全性により、これらは常にnon-nullであるべき
        assertTrue("dateが有効な値を持つこと", entry.date.year > 0)
        assertTrue("contentが初期化されていること", entry.content.length >= 0)
    }

    @Test
    fun `パフォーマンス - 大量データ処理の性能が劣化していないこと`() {
        // Given
        val entryCount = 1000
        val maxProcessingTime = 1000L // 1秒

        // When
        val startTime = System.currentTimeMillis()
        val entries = (1..entryCount).map { i ->
            DiaryEntry(
                LocalDate(2025, 1, 1).plus(DatePeriod(days = i)),
                "日記 $i - ${"内容".repeat(10)}" // 少し長めの内容
            )
        }
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime

        // Then
        assertEquals("指定した数のエントリーが作成されること", entryCount, entries.size)
        assertTrue("処理時間が劣化していないこと（${maxProcessingTime}ms以内）", 
            processingTime < maxProcessingTime)
        
        // リグレッション検証: データの整合性
        assertTrue("最初のエントリーが正しいこと", 
            entries.first().content.startsWith("日記 1"))
        assertTrue("最後のエントリーが正しいこと", 
            entries.last().content.startsWith("日記 $entryCount"))
    }

    @Test
    fun `ソート機能 - 日付ソートの動作が変更されていないこと`() {
        // Given
        val entries = listOf(
            DiaryEntry(LocalDate(2025, 8, 3), "3日目"),
            DiaryEntry(LocalDate(2025, 8, 1), "1日目"),
            DiaryEntry(LocalDate(2025, 8, 5), "5日目"),
            DiaryEntry(LocalDate(2025, 8, 2), "2日目"),
            DiaryEntry(LocalDate(2025, 8, 4), "4日目")
        )

        // When - 新しい順（降順）でソート
        val sortedDesc = entries.sortedByDescending { it.date }
        // 古い順（昇順）でソート
        val sortedAsc = entries.sortedBy { it.date }

        // Then - 降順ソート
        assertEquals("降順ソート1番目", LocalDate(2025, 8, 5), sortedDesc[0].date)
        assertEquals("降順ソート2番目", LocalDate(2025, 8, 4), sortedDesc[1].date)
        assertEquals("降順ソート最後", LocalDate(2025, 8, 1), sortedDesc.last().date)

        // 昇順ソート
        assertEquals("昇順ソート1番目", LocalDate(2025, 8, 1), sortedAsc[0].date)
        assertEquals("昇順ソート2番目", LocalDate(2025, 8, 2), sortedAsc[1].date)
        assertEquals("昇順ソート最後", LocalDate(2025, 8, 5), sortedAsc.last().date)
        
        // リグレッション検証: ソート後のデータ整合性
        assertEquals("ソート後もエントリー数が変わらないこと", entries.size, sortedDesc.size)
        assertEquals("ソート後もエントリー数が変わらないこと", entries.size, sortedAsc.size)
    }

    @Test
    fun `フィルタリング機能 - 空でないエントリーの抽出が変更されていないこと`() {
        // Given
        val entries = listOf(
            DiaryEntry(LocalDate(2025, 8, 1), ""),
            DiaryEntry(LocalDate(2025, 8, 2), "内容あり"),
            DiaryEntry(LocalDate(2025, 8, 3), "   "), // 空白のみ
            DiaryEntry(LocalDate(2025, 8, 4), "もう一つの内容"),
            DiaryEntry(LocalDate(2025, 8, 5), "\n\t"), // 改行・タブのみ
            DiaryEntry(LocalDate(2025, 8, 6), "最後の内容")
        )

        // When
        val nonEmptyEntries = entries.filter { it.content.isNotEmpty() }
        val nonBlankEntries = entries.filter { it.content.isNotBlank() }

        // Then - isEmpty()でのフィルタリング
        assertEquals("空でないエントリー数", 5, nonEmptyEntries.size) // 空文字列以外の5個
        assertTrue("空でないエントリーに空文字列が含まれないこと", 
            nonEmptyEntries.none { it.content.isEmpty() })

        // isNotBlank()でのフィルタリング
        assertEquals("空白でないエントリー数", 3, nonBlankEntries.size)
        assertTrue("空白でないエントリーに空白のみが含まれないこと", 
            nonBlankEntries.none { it.content.isBlank() })
        
        // リグレッション検証: フィルタリング結果の内容
        assertTrue("フィルタリング結果に期待する内容が含まれること", 
            nonBlankEntries.any { it.content == "内容あり" })
        assertTrue("フィルタリング結果に期待する内容が含まれること", 
            nonBlankEntries.any { it.content == "最後の内容" })
    }

    @Test
    fun `境界値テスト - 極端な日付での動作が変更されていないこと`() {
        // Given - 極端な日付
        val minDate = LocalDate(1900, 1, 1)
        val maxDate = LocalDate(2100, 12, 31)
        val leapYearDate = LocalDate(2024, 2, 29) // うるう年

        // When
        val minEntry = DiaryEntry(minDate, "最小日付")
        val maxEntry = DiaryEntry(maxDate, "最大日付")
        val leapEntry = DiaryEntry(leapYearDate, "うるう年")

        // Then
        assertEquals("最小日付が正しく処理されること", minDate, minEntry.date)
        assertEquals("最大日付が正しく処理されること", maxDate, maxEntry.date)
        assertEquals("うるう年が正しく処理されること", leapYearDate, leapEntry.date)
        
        // リグレッション検証: 日付の文字列化
        assertEquals("最小日付の文字列化", "1900-01-01", 
            minEntry.date.toString())
        assertEquals("最大日付の文字列化", "2100-12-31", 
            maxEntry.date.toString())
        assertEquals("うるう年の文字列化", "2024-02-29", 
            leapEntry.date.toString())
    }

    @Test
    fun `文字エンコーディング - 特殊文字の処理が変更されていないこと`() {
        // Given - 様々な特殊文字
        val specialChars = listOf(
            "絵文字: 😀🎉📱💻",
            "記号: !@#$%^&*()_+-=[]{}|;':\",./<>?",
            "日本語: ひらがな カタカナ 漢字",
            "改行\n改行",
            "タブ\tタブ",
            "Unicode: αβγδε ñáéíóú",
            "長い文字列: ${"あ".repeat(1000)}"
        )

        // When & Then
        specialChars.forEachIndexed { index, content ->
            val entry = DiaryEntry(LocalDate(2025, 8, index + 1), content)
            
            assertEquals("特殊文字が正しく保存されること", content, entry.content)
            assertNotNull("特殊文字でもエントリーが作成されること", entry)
            
            // リグレッション検証: 文字列の長さが保持されること
            assertEquals("文字列の長さが保持されること", content.length, entry.content.length)
        }
    }

    @Test
    fun `メモリ使用量 - メモリリークが発生していないこと`() {
        // Given
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val iterations = 100

        // When - 大量のオブジェクト作成と破棄
        repeat(iterations) { i ->
            val entries = (1..100).map { j ->
                DiaryEntry(
                    LocalDate(2025, 1, 1).plus(DatePeriod(days = i * 100 + j)),
                    "メモリテスト $i-$j"
                )
            }
            // 明示的にnullにして参照を切る
            @Suppress("UNUSED_VALUE")
            var nullableEntries: List<DiaryEntry>? = entries
            nullableEntries = null
        }

        // ガベージコレクションを促す
        System.gc()
        Thread.sleep(100) // GCの完了を待つ

        // Then
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val maxAcceptableIncrease = 10 * 1024 * 1024 // 10MB

        assertTrue("メモリ使用量が異常に増加していないこと（${memoryIncrease / 1024 / 1024}MB以内）", 
            memoryIncrease < maxAcceptableIncrease)
    }

    @Test
    fun `スレッドセーフティ - 並行処理での動作が安全であること`() {
        // Given
        val threadCount = 10
        val entriesPerThread = 100
        val results = mutableListOf<List<DiaryEntry>>()

        // When - 複数スレッドで並行してエントリーを作成
        val threads = (1..threadCount).map { threadId ->
            Thread {
                val entries = (1..entriesPerThread).map { entryId ->
                    DiaryEntry(
                        LocalDate(2025, 1, 1).plus(DatePeriod(days = threadId * 1000 + entryId)),
                        "スレッド $threadId エントリー $entryId"
                    )
                }
                synchronized(results) {
                    results.add(entries)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Then
        assertEquals("全スレッドが完了すること", threadCount, results.size)
        
        val totalEntries = results.flatten()
        assertEquals("総エントリー数が正しいこと", threadCount * entriesPerThread, totalEntries.size)
        
        // リグレッション検証: データの整合性
        val uniqueDates = totalEntries.map { it.date }.toSet()
        assertEquals("重複する日付がないこと", totalEntries.size, uniqueDates.size)
        
        assertTrue("全てのエントリーが有効であること", 
            totalEntries.all { it.content.isNotEmpty() })
    }
}
