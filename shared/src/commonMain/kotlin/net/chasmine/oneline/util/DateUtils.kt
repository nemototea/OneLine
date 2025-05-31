package net.chasmine.oneline.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateOrNull

object DateUtils {
    // yyyy-MM-dd形式のバリデーション
    fun isValidDateFormat(fileName: String, dateFormat: String = "yyyy-MM-dd"): Boolean {
        return try {
            val date = LocalDate.parse(fileName)
            date != null
        } catch (e: Exception) {
            false
        }
    }
}
