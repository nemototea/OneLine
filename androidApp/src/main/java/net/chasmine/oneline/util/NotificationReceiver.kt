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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        private const val DEFAULT_NOTIFICATION_TITLE = "今日の一行を書きませんか？"
        private const val DEFAULT_NOTIFICATION_BODY = "今日はどんな一日でしたか？日記を書いて記録しましょう。"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "通知受信: ${intent.action}")

        val notificationManager = AndroidNotificationManager(context)
        val repositoryManager = RepositoryManager.getInstance(context)
        val notificationPrefs = NotificationPreferences.getInstance(context)

        // 今日の日記が既に書かれているかチェック
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // RepositoryManagerを使用して、現在のモードに応じた日記をチェック
                val entry = repositoryManager.getEntry(today)
                val shouldShowNotification = entry == null || entry.content.isBlank()

                Log.d(TAG, "今日の日記チェック - 日付: $today, 通知表示: $shouldShowNotification")

                if (shouldShowNotification) {
                    // 今日の日記がまだ書かれていない場合のみ通知を表示
                    notificationManager.showNotification(
                        DEFAULT_NOTIFICATION_TITLE,
                        DEFAULT_NOTIFICATION_BODY
                    )
                } else {
                    Log.d(TAG, "今日の日記は既に書かれているため通知をスキップします")
                }

                // 次の日の通知をスケジュール（重要：継続的な通知のため）
                val isEnabled = notificationPrefs.isNotificationEnabled.value
                if (isEnabled) {
                    val hour = notificationPrefs.notificationHour.value
                    val minute = notificationPrefs.notificationMinute.value

                    Log.d(TAG, "次回通知をスケジュール: ${hour}:${minute}")
                    notificationManager.scheduleDailyNotification(hour, minute)
                } else {
                    Log.d(TAG, "通知が無効になっているため、次回通知をスケジュールしません")
                }

            } catch (e: Exception) {
                Log.e(TAG, "通知処理中にエラーが発生しました", e)
                // エラーが発生した場合でも通知を表示
                notificationManager.showNotification(
                    DEFAULT_NOTIFICATION_TITLE,
                    DEFAULT_NOTIFICATION_BODY
                )

                // 次回通知もスケジュール
                try {
                    val isEnabled = notificationPrefs.isNotificationEnabled.value
                    if (isEnabled) {
                        val hour = notificationPrefs.notificationHour.value
                        val minute = notificationPrefs.notificationMinute.value
                        notificationManager.scheduleDailyNotification(hour, minute)
                    }
                } catch (scheduleError: Exception) {
                    Log.e(TAG, "次回通知のスケジュールに失敗しました", scheduleError)
                }
            }
        }
    }
}
