package net.chasmine.oneline.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.git.GitRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = DiaryNotificationManager(context)
        val gitRepository = GitRepository.getInstance(context)
        
        // 今日の日記が既に書かれているかチェック
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entry = gitRepository.getEntry(today)
                if (entry == null || entry.content.isBlank()) {
                    // 今日の日記がまだ書かれていない場合のみ通知を表示
                    notificationManager.showNotification()
                }
                
                // 次の日の通知をスケジュール
                val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val hour = prefs.getInt("notification_hour", 20) // デフォルト20時
                val minute = prefs.getInt("notification_minute", 0) // デフォルト0分
                notificationManager.scheduleDaily(hour, minute)
                
            } catch (e: Exception) {
                // エラーが発生した場合でも通知を表示
                notificationManager.showNotification()
            }
        }
    }
}
