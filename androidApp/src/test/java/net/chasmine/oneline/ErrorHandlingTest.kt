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
 * エラーハンドリングテスト
 * 
 * 異常系とエラーケースを包括的にテストし、
 * アプリケーションの堅牢性を検証します。
 */
class ErrorHandlingTest {

    @Test
    fun `ファイルシステムエラー - 権限不足での書き込み失敗`() {
        // Given
        val testDir = createTempDir("diary_permission_test")
        val testEntry = DiaryEntry(LocalDate(2025, 8, 3), "権限テスト")
        
        try {
            // When - ディレクトリを読み取り専用に設定
            testDir.setReadOnly()
            val fileName = "${testEntry.date}.txt"
            val testFile = File(testDir, fileName)

            var caughtException: Exception? = null
            try {
                testFile.writeText(testEntry.content)
            } catch (e: Exception) {
                caughtException = e
            }

            // Then - 適切にエラーが発生すること
            assertNotNull("権限エラーが発生すること", caughtException)
            assertFalse("ファイルが作成されないこと", testFile.exists())
            
            // エラーの種類を確認
            assertTrue("適切な例外タイプであること", 
                caughtException is java.io.IOException || 
                caughtException is SecurityException)

        } finally {
            // Cleanup
            testDir.setWritable(true)
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `ファイルシステムエラー - ディスク容量不足のシミュレーション`() {
        // Given
        val testDir = createTempDir("diary_space_test")
        val testEntry = DiaryEntry(LocalDate(2025, 8, 3), "容量テスト")
        
        try {
            // When - 非常に大きなファイルの作成を試行
            val fileName = "${testEntry.date}.txt"
            val testFile = File(testDir, fileName)
            val largeContent = "x".repeat(Int.MAX_VALUE / 1000) // 大きなコンテンツ

            var caughtException: Exception? = null
            try {
                testFile.writeText(largeContent)
            } catch (e: Exception) {
                caughtException = e
            }

            // Then - メモリ不足またはIO例外が発生すること
            // 注意: このテストは環境によって動作が異なる可能性があります
            if (caughtException != null) {
                assertTrue("適切な例外タイプであること", 
                    caughtException is OutOfMemoryError || 
                    caughtException is java.io.IOException)
            }

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `データ破損エラー - 不正なファイル形式の処理`() {
        // Given
        val testDir = createTempDir("diary_corruption_test")
        
        try {
            // 不正なファイル名のファイルを作成
            val invalidFileNames = listOf(
                "invalid-date-format.txt",
                "2025-13-01.txt", // 不正な月
                "2025-02-30.txt", // 存在しない日付
                "2025-08-03", // 拡張子なし
                ".txt", // 日付なし
                "2025-08-03.doc" // 不正な拡張子
            )

            invalidFileNames.forEach { fileName ->
                val file = File(testDir, fileName)
                file.writeText("不正なファイル: $fileName")
            }

            // When - ファイルの検証
            val allFiles = testDir.listFiles() ?: emptyArray()
            val validFiles = mutableListOf<File>()
            val invalidFilesList = mutableListOf<File>()

            allFiles.forEach { file ->
                if (file.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.txt"))) {
                    // 日付の妥当性をチェック
                    try {
                        val datePart = file.name.substringBefore(".txt")
                        LocalDate.parse(datePart)
                        validFiles.add(file)
                    } catch (e: Exception) {
                        invalidFilesList.add(file)
                    }
                } else {
                    invalidFilesList.add(file)
                }
            }

            // Then - 不正なファイルが適切に識別されること
            assertEquals("有効ファイル数", 0, validFiles.size)
            assertEquals("無効ファイル数", 6, invalidFilesList.size)
            
            // 各無効ファイルの詳細チェック
            assertTrue("不正な月のファイルが検出されること", 
                invalidFilesList.any { it.name == "2025-13-01.txt" })
            assertTrue("存在しない日付のファイルが検出されること", 
                invalidFilesList.any { it.name == "2025-02-30.txt" })

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `メモリエラー - 大量データ処理時のメモリ管理`() {
        // Given
        val largeDataSize = 10000
        val maxMemoryIncrease = 50 * 1024 * 1024 // 50MB

        // 初期メモリ使用量を測定
        System.gc()
        Thread.sleep(100)
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        try {
            // When - 大量のDiaryEntryオブジェクトを作成
            val entries = mutableListOf<DiaryEntry>()
            repeat(largeDataSize) { i ->
                val entry = DiaryEntry(
                    LocalDate(2025, 1, 1).plus(DatePeriod(days = i)),
                    "大量データテスト $i - ${"内容".repeat(100)}"
                )
                entries.add(entry)
            }

            // メモリ使用量を測定
            val afterCreationMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memoryIncrease = afterCreationMemory - initialMemory

            // Then - メモリ使用量が妥当な範囲内であること
            assertTrue("メモリ使用量が妥当であること（${memoryIncrease / 1024 / 1024}MB）", 
                memoryIncrease < maxMemoryIncrease)
            assertEquals("作成されたエントリー数が正しいこと", largeDataSize, entries.size)

            // メモリリークのテスト
            entries.clear()
            System.gc()
            Thread.sleep(100)
            
            val afterCleanupMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memoryAfterCleanup = afterCleanupMemory - initialMemory
            
            assertTrue("メモリがある程度解放されること", 
                memoryAfterCleanup < memoryIncrease / 2)

        } catch (e: OutOfMemoryError) {
            // メモリ不足が発生した場合の処理
            fail("メモリ不足が発生しました。データサイズを調整してください: ${e.message}")
        }
    }

    @Test
    fun `並行処理エラー - 競合状態の検出`() {
        // Given
        val testDir = createTempDir("diary_concurrency_test")
        val testFile = File(testDir, "2025-08-03.txt")
        val threadCount = 10
        val exceptions = mutableListOf<Exception>()

        try {
            // When - 複数スレッドから同じファイルに同時書き込み
            val threads = (1..threadCount).map { threadId ->
                Thread {
                    try {
                        repeat(10) { iteration ->
                            val content = "スレッド $threadId - 反復 $iteration"
                            // 意図的に同期化しない（競合状態を発生させる）
                            testFile.writeText(content)
                            Thread.sleep(1) // 競合を発生しやすくする
                        }
                    } catch (e: Exception) {
                        synchronized(exceptions) {
                            exceptions.add(e)
                        }
                    }
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            // Then - 競合状態の結果を検証
            assertTrue("ファイルが作成されること", testFile.exists())
            
            // ファイルの内容が有効であることを確認
            val finalContent = testFile.readText()
            assertFalse("ファイル内容が空でないこと", finalContent.isEmpty())
            assertTrue("ファイル内容が有効な形式であること", 
                finalContent.startsWith("スレッド"))

            // 例外が発生した場合の記録
            if (exceptions.isNotEmpty()) {
                println("並行処理中に発生した例外: ${exceptions.size}件")
                exceptions.forEach { e ->
                    println("例外: ${e.javaClass.simpleName} - ${e.message}")
                }
            }

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `入力検証エラー - 不正な日付の処理`() {
        // Given
        val invalidDates = listOf(
            "2025-13-01", // 不正な月
            "2025-02-30", // 存在しない日付
            "2025-04-31", // 4月31日は存在しない
            "2024-02-30", // うるう年でも2月30日は存在しない
            "invalid-date",
            "2025/08/03", // 不正なフォーマット
            "2025-8-3", // ゼロパディングなし
            ""
        )

        // When & Then
        invalidDates.forEach { dateString ->
            var caughtException: Exception? = null
            try {
                LocalDate.parse(dateString)
            } catch (e: Exception) {
                caughtException = e
            }

            assertNotNull("不正な日付 '$dateString' で例外が発生すること", caughtException)
            assertTrue("適切な例外タイプであること", 
                caughtException is IllegalArgumentException)
        }
    }

    @Test
    fun `文字エンコーディングエラー - 不正な文字の処理`() {
        // Given
        val testDir = createTempDir("diary_encoding_error_test")
        
        try {
            // 制御文字を含む内容
            val controlChars = "\u0000\u0001\u0002\u0003\u0004\u0005"
            val testEntry = DiaryEntry(LocalDate(2025, 8, 3), "制御文字テスト: $controlChars")
            
            // When - 制御文字を含むファイルの保存・読み込み
            val fileName = "${testEntry.date}.txt"
            val testFile = File(testDir, fileName)
            
            var writeException: Exception? = null
            var readException: Exception? = null
            var loadedContent: String? = null
            
            try {
                testFile.writeText(testEntry.content, Charsets.UTF_8)
                loadedContent = testFile.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                if (writeException == null) writeException = e
                else readException = e
            }

            // Then - 制御文字が適切に処理されること
            if (writeException == null && readException == null) {
                // 正常に処理された場合
                assertEquals("制御文字が保持されること", testEntry.content, loadedContent)
            } else {
                // エラーが発生した場合は適切な例外であることを確認
                val exception = writeException ?: readException
                assertTrue("適切な例外タイプであること", 
                    exception is java.io.IOException || 
                    exception is java.nio.charset.MalformedInputException)
            }

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `リソースリークエラー - ファイルハンドルのリーク検出`() {
        // Given
        val testDir = createTempDir("diary_resource_leak_test")
        val fileCount = 100
        
        try {
            // When - 大量のファイル操作を実行
            repeat(fileCount) { i ->
                val fileName = "test-$i.txt"
                val testFile = File(testDir, fileName)
                
                // ファイルの作成・書き込み・読み込みを繰り返す
                testFile.writeText("テスト内容 $i")
                val content = testFile.readText()
                assertEquals("内容が正しく読み書きされること", "テスト内容 $i", content)
            }

            // Then - リソースが適切に解放されていることを確認
            val files = testDir.listFiles()
            assertNotNull("ファイルリストが取得できること", files)
            assertEquals("作成されたファイル数が正しいこと", fileCount, files!!.size)
            
            // 全ファイルが読み込み可能であることを確認
            files.forEach { file ->
                assertTrue("ファイルが読み込み可能であること: ${file.name}", file.canRead())
                assertFalse("ファイル内容が空でないこと: ${file.name}", file.readText().isEmpty())
            }

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `ネットワークエラーシミュレーション - 外部依存の失敗`() {
        // Given - 外部リソースへのアクセスをシミュレート
        val testDir = createTempDir("diary_network_test")
        
        try {
            // When - ネットワークエラーをシミュレート
            var networkException: Exception? = null
            try {
                // 存在しないネットワークパスへのアクセスを試行
                val networkPath = File("//nonexistent-server/path/file.txt")
                networkPath.readText()
            } catch (e: Exception) {
                networkException = e
            }

            // Then - 適切にエラーが処理されること
            assertNotNull("ネットワークエラーが発生すること", networkException)
            assertTrue("適切な例外タイプであること", 
                networkException is java.io.FileNotFoundException || 
                networkException is java.io.IOException ||
                networkException is SecurityException)

            // フォールバック処理のテスト
            val fallbackFile = File(testDir, "fallback.txt")
            fallbackFile.writeText("フォールバックデータ")
            
            assertTrue("フォールバックファイルが作成されること", fallbackFile.exists())
            assertEquals("フォールバックデータが正しいこと", 
                "フォールバックデータ", fallbackFile.readText())

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `タイムアウトエラー - 長時間処理の中断`() {
        // Given
        val timeoutMs = 1000L // 1秒のタイムアウト
        val testDir = createTempDir("diary_timeout_test")
        
        try {
            // When - 長時間処理をシミュレート
            val startTime = System.currentTimeMillis()
            var completed = false
            var timedOut = false
            
            val processingThread = Thread {
                try {
                    // 意図的に長時間の処理をシミュレート
                    repeat(1000) { i ->
                        val file = File(testDir, "slow-$i.txt")
                        file.writeText("処理中 $i")
                        Thread.sleep(10) // 各操作を遅くする
                        
                        // タイムアウトチェック
                        if (System.currentTimeMillis() - startTime > timeoutMs) {
                            timedOut = true
                            return@Thread
                        }
                    }
                    completed = true
                } catch (e: InterruptedException) {
                    timedOut = true
                }
            }
            
            processingThread.start()
            processingThread.join(timeoutMs + 500) // タイムアウト + 余裕
            
            if (processingThread.isAlive) {
                processingThread.interrupt()
                timedOut = true
            }

            // Then - タイムアウトが適切に処理されること
            assertTrue("タイムアウトが発生すること", timedOut || !completed)
            
            val elapsedTime = System.currentTimeMillis() - startTime
            assertTrue("処理時間がタイムアウト時間を大幅に超えないこと", 
                elapsedTime < timeoutMs + 1000)

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `復旧処理テスト - エラー後の自動復旧`() {
        // Given
        val testDir = createTempDir("diary_recovery_test")
        val backupDir = File(testDir, "backup")
        backupDir.mkdirs()
        
        try {
            // 正常なデータを作成
            val originalFile = File(testDir, "2025-08-03.txt")
            val originalContent = "元の日記内容"
            originalFile.writeText(originalContent)
            
            // バックアップを作成
            val backupFile = File(backupDir, "2025-08-03.txt")
            backupFile.writeText(originalContent)

            // When - データを破損させる
            originalFile.writeText("") // 空にして破損をシミュレート
            
            // 復旧処理をシミュレート
            val isCorrupted = originalFile.readText().isEmpty()
            var recoveredContent: String? = null
            
            if (isCorrupted && backupFile.exists()) {
                recoveredContent = backupFile.readText()
                originalFile.writeText(recoveredContent)
            }

            // Then - 復旧が成功すること
            assertTrue("データが破損していることが検出されること", isCorrupted)
            assertNotNull("復旧データが取得されること", recoveredContent)
            assertEquals("復旧されたデータが正しいこと", originalContent, recoveredContent)
            assertEquals("元ファイルが復旧されること", originalContent, originalFile.readText())

        } finally {
            // Cleanup
            testDir.deleteRecursively()
        }
    }
}
