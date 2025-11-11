package net.chasmine.oneline.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * DiaryEntry のテスト
 */
class DiaryEntryTest {

    @Test
    fun testDiaryEntryCreation() {
        val date = LocalDate(2025, 11, 11)
        val content = "今日はテストを書きました。"
        val entry = DiaryEntry(date = date, content = content)

        assertEquals(date, entry.date)
        assertEquals(content, entry.content)
    }

    @Test
    fun testGetFileName() {
        val date = LocalDate(2025, 11, 11)
        val entry = DiaryEntry(date = date, content = "Test")

        assertEquals("2025-11-11.md", entry.getFileName())
    }

    @Test
    fun testGetFileNameWithSingleDigits() {
        val date = LocalDate(2025, 1, 5)
        val entry = DiaryEntry(date = date, content = "Test")

        assertEquals("2025-01-05.md", entry.getFileName())
    }

    @Test
    fun testGetDisplayDate() {
        val date = LocalDate(2025, 11, 11)
        val entry = DiaryEntry(date = date, content = "Test")

        assertEquals("2025年11月11日", entry.getDisplayDate())
    }

    @Test
    fun testGetDisplayDateWithSingleDigits() {
        val date = LocalDate(2025, 1, 5)
        val entry = DiaryEntry(date = date, content = "Test")

        assertEquals("2025年1月5日", entry.getDisplayDate())
    }

    @Test
    fun testDataClassEquality() {
        val date = LocalDate(2025, 11, 11)
        val entry1 = DiaryEntry(date = date, content = "Same content", lastModified = 1000L)
        val entry2 = DiaryEntry(date = date, content = "Same content", lastModified = 1000L)

        assertEquals(entry1, entry2)
    }

    @Test
    fun testDataClassInequality() {
        val date = LocalDate(2025, 11, 11)
        val entry1 = DiaryEntry(date = date, content = "Content 1")
        val entry2 = DiaryEntry(date = date, content = "Content 2")

        assertNotEquals(entry1, entry2)
    }

    @Test
    fun testDataClassCopy() {
        val date = LocalDate(2025, 11, 11)
        val original = DiaryEntry(date = date, content = "Original")
        val copy = original.copy(content = "Modified")

        assertEquals(date, copy.date)
        assertEquals("Modified", copy.content)
        assertNotEquals(original.content, copy.content)
    }

    @Test
    fun testSerialization() {
        val date = LocalDate(2025, 11, 11)
        val entry = DiaryEntry(date = date, content = "Test content", lastModified = 1000L)

        val json = Json.encodeToString(DiaryEntry.serializer(), entry)
        val decoded = Json.decodeFromString(DiaryEntry.serializer(), json)

        assertEquals(entry.date, decoded.date)
        assertEquals(entry.content, decoded.content)
        assertEquals(entry.lastModified, decoded.lastModified)
    }

    @Test
    fun testSerializationWithMultilineContent() {
        val date = LocalDate(2025, 11, 11)
        val multilineContent = """
            今日は良い一日でした。
            テストコードを書いて、
            品質を向上させました。
        """.trimIndent()
        val entry = DiaryEntry(date = date, content = multilineContent, lastModified = 2000L)

        val json = Json.encodeToString(DiaryEntry.serializer(), entry)
        val decoded = Json.decodeFromString(DiaryEntry.serializer(), json)

        assertEquals(entry.content, decoded.content)
    }

    @Test
    fun testEmptyContent() {
        val date = LocalDate(2025, 11, 11)
        val entry = DiaryEntry(date = date, content = "")

        assertEquals("", entry.content)
        assertEquals("2025-11-11.md", entry.getFileName())
    }
}
