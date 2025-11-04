package net.chasmine.oneline.util

import kotlinx.datetime.LocalDate

object DateUtils {

    /**
     * ファイル名が指定された日付フォーマットに一致するかを検証
     * @param fileName ファイル名
     * @param dateFormat 日付フォーマット (現在は "yyyy-MM-dd" のみサポート)
     * @return 一致する場合はtrue、それ以外はfalse
     */
    fun isValidDateFormat(fileName: String, dateFormat: String): Boolean {
        // kotlinx.datetime は ISO 8601 フォーマット ("yyyy-MM-dd") のみサポート
        if (dateFormat != "yyyy-MM-dd") {
            return false
        }

        return try {
            // ISO 8601 フォーマットでパース試行
            LocalDate.parse(fileName)
            true
        } catch (e: Exception) {
            false
        }
    }
}
