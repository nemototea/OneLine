package net.chasmine.oneline.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.chasmine.oneline.R
import net.chasmine.oneline.ui.MainActivity
import java.util.*

class DiaryNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DiaryNotificationManager"
        private const val CHANNEL_ID = "diary_reminder"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE = 1002
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        val name = "日記リマインダー"
        val descriptionText = "日記を書くリマインダー通知"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
            enableLights(true)
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        Log.d(TAG, "通知チャンネルを作成しました: $CHANNEL_ID")
    }
    
    fun scheduleDaily(hour: Int, minute: Int) {
        Log.d(TAG, "日次通知をスケジュール: ${hour}:${minute}")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Android 12以降のアラーム権限チェック
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "正確なアラームの権限がありません")
                // 権限がない場合は通常のアラームを使用
                scheduleInexactAlarm(hour, minute)
                return
            }
        }
        
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // 今日の指定時刻が過ぎていたら明日に設定
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        Log.d(TAG, "次回通知予定時刻: ${calendar.time}")
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "アラームを正常にスケジュールしました")
        } catch (e: Exception) {
            Log.e(TAG, "アラームのスケジュールに失敗しました", e)
        }
    }
    
    private fun scheduleInexactAlarm(hour: Int, minute: Int) {
        Log.d(TAG, "不正確なアラームを使用します: ${hour}:${minute}")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        // 不正確なアラームを使用（バッテリー最適化の影響を受ける可能性あり）
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    
    fun cancelDaily() {
        Log.d(TAG, "日次通知をキャンセルします")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "アラームをキャンセルしました")
    }
    
    fun showNotification() {
        Log.d(TAG, "通知を表示します")
        
        // 通知権限チェック
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "通知権限がありません")
                return
            }
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("今日の一行を書きませんか？")
            .setContentText("今日はどんな一日でしたか？日記を書いて記録しましょう。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "通知を正常に表示しました")
        } catch (e: Exception) {
            Log.e(TAG, "通知の表示に失敗しました", e)
        }
    }
    
    // テスト用：即座に通知を表示
    fun showTestNotification() {
        Log.d(TAG, "テスト通知を表示します")
        showNotification()
    }
}
