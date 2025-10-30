package net.chasmine.oneline.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.repository.RepositoryManager
import net.chasmine.oneline.data.preferences.NotificationPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "通知受信: ${intent.action}")

        val notificationManager = DiaryNotificationManager(context)
        val repositoryManager = RepositoryManager.getInstance(context)
        val notificationPrefs = NotificationPreferences.getInstance(context)

        // 今日の日記が既に書かれているかチェック
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // RepositoryManagerを使用して、現在のモードに応じた日記をチェック
                val entry = repositoryManager.getEntry(today)
                val shouldShowNotification = entry == null || entry.content.isBlank()

                Log.d(TAG, "今日の日記チェック - 日付: $today, 通知表示: $shouldShowNotification")

                if (shouldShowNotification) {
                    // 今日の日記がまだ書かれていない場合のみ通知を表示
                    notificationManager.showNotification()
                } else {
                    Log.d(TAG, "今日の日記は既に書かれているため通知をスキップします")
                }

                // 次の日の通知をスケジュール（重要：継続的な通知のため）
                val isEnabled = notificationPrefs.isNotificationEnabled.value
                if (isEnabled) {
                    val hour = notificationPrefs.notificationHour.value
                    val minute = notificationPrefs.notificationMinute.value

                    Log.d(TAG, "次回通知をスケジュール: ${hour}:${minute}")
                    notificationManager.scheduleDaily(hour, minute)
                } else {
                    Log.d(TAG, "通知が無効になっているため、次回通知をスケジュールしません")
                }

            } catch (e: Exception) {
                Log.e(TAG, "通知処理中にエラーが発生しました", e)
                // エラーが発生した場合でも通知を表示
                notificationManager.showNotification()

                // 次回通知もスケジュール
                try {
                    val isEnabled = notificationPrefs.isNotificationEnabled.value
                    if (isEnabled) {
                        val hour = notificationPrefs.notificationHour.value
                        val minute = notificationPrefs.notificationMinute.value
                        notificationManager.scheduleDaily(hour, minute)
                    }
                } catch (scheduleError: Exception) {
                    Log.e(TAG, "次回通知のスケジュールに失敗しました", scheduleError)
                }
            }
        }
    }
}
