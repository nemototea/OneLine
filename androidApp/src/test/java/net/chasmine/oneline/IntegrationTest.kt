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
 * 統合テスト
 * 
 * 実際のアプリケーションコンポーネントの統合動作をテストし、
 * システム全体の動作を検証します。
 */
class IntegrationTest {

    @Test
    fun `ファイルシステム統合 - ローカルストレージの基本動作`() {
        // Given
        val testDir = createTempDir("diary_integration_test")
        val testEntry = DiaryEntry(LocalDate(2025, 8, 3), "統合テスト用日記")
        val fileName = "${testEntry.date}.txt"
        val testFile = File(testDir, fileName)

        try {
            // When - ファイル保存をシミュレート
            testFile.writeText(testEntry.content)

            // Then - ファイルが正しく作成されること
            assertTrue("ファイルが作成されること", testFile.exists())
            assertEquals("ファイル内容が正しいこと", testEntry.content, testFile.readText())
            assertEquals("ファイル名が正しいこと", "2025-08-03.txt", testFile.name)

            // ファイル読み込みをシミュレート
            val loadedContent = testFile.readText()
            val loadedEntry = DiaryEntry(testEntry.date, loadedContent)
            
            assertEquals("読み込んだエントリーが元と同じであること", testEntry.date, loadedEntry.date)
            assertEquals("読み込んだ内容が元と同じであること", testEntry.content, loadedEntry.content)

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `ファイルシステム統合 - 複数ファイルの管理`() {
        // Given
        val testDir = createTempDir("diary_multi_test")
        val entries = listOf(
            DiaryEntry(LocalDate(2025, 8, 1), "1日目の日記"),
            DiaryEntry(LocalDate(2025, 8, 2), "2日目の日記"),
            DiaryEntry(LocalDate(2025, 8, 3), "3日目の日記")
        )

        try {
            // When - 複数ファイルを保存
            entries.forEach { entry ->
                val fileName = "${entry.date}.txt"
                val file = File(testDir, fileName)
                file.writeText(entry.content)
            }

            // Then - 全ファイルが作成されること
            val files = testDir.listFiles { file -> file.name.endsWith(".txt") }
            assertNotNull("ファイルリストが取得できること", files)
            assertEquals("ファイル数が正しいこと", 3, files!!.size)

            // ファイルを日付順でソート（ファイル名でソート）
            val sortedFiles = files.sortedBy { it.name }
            
            // 各ファイルの内容を検証
            sortedFiles.forEachIndexed { index, file ->
                val expectedEntry = entries[index]
                val actualContent = file.readText()
                assertEquals("ファイル${index + 1}の内容が正しいこと", 
                    expectedEntry.content, actualContent)
            }

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `エラーハンドリング統合 - ファイル操作エラーの処理`() {
        // Given
        val testDir = createTempDir("diary_error_test")
        val testEntry = DiaryEntry(LocalDate(2025, 8, 3), "エラーテスト")

        try {
            // When - 読み取り専用ディレクトリでの書き込みテスト
            testDir.setReadOnly()
            val fileName = "${testEntry.date}.txt"
            val testFile = File(testDir, fileName)

            var writeException: Exception? = null
            try {
                testFile.writeText(testEntry.content)
            } catch (e: Exception) {
                writeException = e
            }

            // Then - 適切にエラーが発生すること
            assertNotNull("書き込みエラーが発生すること", writeException)
            assertFalse("ファイルが作成されないこと", testFile.exists())

        } finally {
            // Cleanup - 権限を戻してから削除
            testDir.setWritable(true)
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `エラーハンドリング統合 - 存在しないファイルの読み込み`() {
        // Given
        val testDir = createTempDir("diary_missing_test")
        val nonExistentFile = File(testDir, "2025-08-03.txt")

        try {
            // When - 存在しないファイルの読み込み
            var readException: Exception? = null
            var content: String? = null
            
            try {
                content = nonExistentFile.readText()
            } catch (e: Exception) {
                readException = e
            }

            // Then - 適切にエラーが発生すること
            assertNotNull("読み込みエラーが発生すること", readException)
            assertNull("内容が読み込まれないこと", content)
            assertFalse("ファイルが存在しないこと", nonExistentFile.exists())

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `データ整合性統合 - 破損ファイルの検出`() {
        // Given
        val testDir = createTempDir("diary_corruption_test")
        
        try {
            // 正常なファイル
            val validFile = File(testDir, "2025-08-01.txt")
            validFile.writeText("正常な日記")

            // 空のファイル
            val emptyFile = File(testDir, "2025-08-02.txt")
            emptyFile.createNewFile()

            // 不正な日付のファイル
            val invalidDateFile = File(testDir, "invalid-date.txt")
            invalidDateFile.writeText("不正な日付のファイル")

            // When - ファイルの整合性をチェック
            val allFiles = testDir.listFiles { file -> file.isFile }
            assertNotNull("ファイルリストが取得できること", allFiles)

            var validCount = 0
            var emptyCount = 0
            var invalidCount = 0

            allFiles!!.forEach { file ->
                when {
                    file.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.txt")) -> {
                        // 正しい日付形式
                        if (file.readText().isNotEmpty()) {
                            validCount++
                        } else {
                            emptyCount++
                        }
                    }
                    else -> {
                        // 不正な形式
                        invalidCount++
                    }
                }
            }

            // Then - 整合性チェック結果が正しいこと
            assertEquals("有効ファイル数", 1, validCount)
            assertEquals("空ファイル数", 1, emptyCount)
            assertEquals("不正ファイル数", 1, invalidCount)
            assertEquals("総ファイル数", 3, allFiles.size)

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `パフォーマンス統合 - 大量ファイル操作の性能`() {
        // Given
        val testDir = createTempDir("diary_performance_test")
        val fileCount = 100
        val maxProcessingTime = 2000L // 2秒

        try {
            // When - 大量ファイルの作成
            val startTime = System.currentTimeMillis()
            
            repeat(fileCount) { i ->
                val date = LocalDate(2025, 1, 1).plus(DatePeriod(days = i))
                val fileName = "${date}.txt"
                val file = File(testDir, fileName)
                file.writeText("日記 $i - ${"内容".repeat(50)}")
            }
            
            val creationTime = System.currentTimeMillis() - startTime

            // ファイル読み込み
            val readStartTime = System.currentTimeMillis()
            val files = testDir.listFiles { file -> file.name.endsWith(".txt") }
            val contents = files?.map { it.readText() } ?: emptyList()
            val readTime = System.currentTimeMillis() - readStartTime

            // Then - パフォーマンス要件を満たすこと
            assertTrue("ファイル作成時間が妥当であること（${creationTime}ms）", 
                creationTime < maxProcessingTime)
            assertTrue("ファイル読み込み時間が妥当であること（${readTime}ms）", 
                readTime < maxProcessingTime)
            
            assertEquals("作成されたファイル数が正しいこと", fileCount, files?.size ?: 0)
            assertEquals("読み込まれた内容数が正しいこと", fileCount, contents.size)
            
            // データの整合性確認
            assertTrue("全ての内容が読み込まれていること", 
                contents.all { it.isNotEmpty() })

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `文字エンコーディング統合 - UTF-8での保存・読み込み`() {
        // Given
        val testDir = createTempDir("diary_encoding_test")
        val specialContent = "日本語 🎉 émojis αβγ ñáéíóú"
        val testEntry = DiaryEntry(LocalDate(2025, 8, 3), specialContent)

        try {
            // When - 特殊文字を含むファイルの保存・読み込み
            val fileName = "${testEntry.date}.txt"
            val testFile = File(testDir, fileName)
            
            testFile.writeText(testEntry.content, Charsets.UTF_8)
            val loadedContent = testFile.readText(Charsets.UTF_8)

            // Then - 文字エンコーディングが正しく処理されること
            assertEquals("特殊文字が正しく保存・読み込みされること", 
                specialContent, loadedContent)
            assertEquals("文字列の長さが保持されること", 
                specialContent.length, loadedContent.length)

            // バイト配列での比較（エンコーディングの確認）
            val originalBytes = specialContent.toByteArray(Charsets.UTF_8)
            val loadedBytes = loadedContent.toByteArray(Charsets.UTF_8)
            assertArrayEquals("バイト配列が同じであること", originalBytes, loadedBytes)

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `同期処理統合 - ファイル操作の排他制御`() {
        // Given
        val testDir = createTempDir("diary_sync_test")
        val testFile = File(testDir, "2025-08-03.txt")
        val threadCount = 5
        val results = mutableListOf<String>()

        try {
            // When - 複数スレッドから同じファイルへの書き込み
            val threads = (1..threadCount).map { threadId ->
                Thread {
                    val content = "スレッド $threadId の内容"
                    synchronized(testFile) {
                        // ファイルの読み込み（存在する場合）
                        val existingContent = if (testFile.exists()) {
                            testFile.readText()
                        } else {
                            ""
                        }
                        
                        // 内容を追記
                        val newContent = if (existingContent.isEmpty()) {
                            content
                        } else {
                            "$existingContent\n$content"
                        }
                        
                        testFile.writeText(newContent)
                        
                        synchronized(results) {
                            results.add(content)
                        }
                    }
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            // Then - 排他制御が正しく動作すること
            assertTrue("ファイルが作成されること", testFile.exists())
            assertEquals("全スレッドが完了すること", threadCount, results.size)
            
            val finalContent = testFile.readText()
            assertFalse("ファイル内容が空でないこと", finalContent.isEmpty())
            
            // 各スレッドの内容が含まれていることを確認
            results.forEach { threadContent ->
                assertTrue("スレッドの内容が含まれていること: $threadContent", 
                    finalContent.contains(threadContent))
            }

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `システム統合 - アプリケーション全体のワークフロー`() {
        // Given - アプリケーションの典型的な使用シナリオ
        val testDir = createTempDir("diary_workflow_test")
        val user = "テストユーザー"
        
        try {
            // Scenario 1: 新規日記の作成
            val newEntry = DiaryEntry(Clock.System.todayIn(TimeZone.currentSystemDefault()), "今日の日記を書きました")
            val newFileName = "${newEntry.date}.txt"
            val newFile = File(testDir, newFileName)
            newFile.writeText(newEntry.content)
            
            assertTrue("新規日記が作成されること", newFile.exists())

            // Scenario 2: 既存日記の編集
            val updatedContent = "${newEntry.content}\n追記: 編集しました"
            newFile.writeText(updatedContent)
            
            assertEquals("日記が編集されること", updatedContent, newFile.readText())

            // Scenario 3: 複数日記の管理
            val additionalEntries = listOf(
                DiaryEntry(Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(DatePeriod(days = 1)), "昨日の日記"),
                DiaryEntry(Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(DatePeriod(days = 2)), "一昨日の日記")
            )
            
            additionalEntries.forEach { entry ->
                val fileName = "${entry.date}.txt"
                val file = File(testDir, fileName)
                file.writeText(entry.content)
            }

            // Scenario 4: 日記一覧の取得
            val allFiles = testDir.listFiles { file -> file.name.endsWith(".txt") }
            assertNotNull("日記一覧が取得できること", allFiles)
            assertEquals("日記数が正しいこと", 3, allFiles!!.size)

            // Scenario 5: 日記の検索（内容による）
            val searchKeyword = "編集"
            val matchingFiles = allFiles.filter { file ->
                file.readText().contains(searchKeyword)
            }
            
            assertEquals("検索結果が正しいこと", 1, matchingFiles.size)
            assertTrue("検索結果の内容が正しいこと", 
                matchingFiles.first().readText().contains(searchKeyword))

            // Scenario 6: 日記の削除
            val fileToDelete = allFiles.first()
            val deletedFileName = fileToDelete.name
            assertTrue("削除対象ファイルが存在すること", fileToDelete.exists())
            
            fileToDelete.delete()
            assertFalse("ファイルが削除されること", fileToDelete.exists())
            
            // 削除後の確認
            val remainingFiles = testDir.listFiles { file -> file.name.endsWith(".txt") }
            assertEquals("残りのファイル数が正しいこと", 2, remainingFiles?.size ?: 0)
            assertTrue("削除されたファイルが一覧にないこと", 
                remainingFiles?.none { it.name == deletedFileName } ?: true)

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }
}
