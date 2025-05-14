package net.chasmine.oneline.util

import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class DateUtils @Inject constructor()
{

    /**
     * ファイル名が指定された日付フォーマットに一致するかを検証
     * @param fileName ファイル名
     * @param dateFormat 日付フォーマット (例: "yyyy-MM-dd")
     * @return 一致する場合はtrue、それ以外はfalse
     */
    fun isValidDateFormat(fileName: String, dateFormat: String): Boolean {
        return try {
            val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
            formatter.isLenient = false
            formatter.parse(fileName)
            true
        } catch (e: Exception) {
            false
        }
    }
}