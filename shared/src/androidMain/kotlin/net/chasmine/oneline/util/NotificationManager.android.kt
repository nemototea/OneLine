package net.chasmine.oneline.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidSystemNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Android implementation of NotificationManager
 *
 * Migrated from DiaryNotificationManager (Phase 6-2)
 * Uses AlarmManager for scheduling notifications
 */
class AndroidNotificationManager(private val context: Context) : NotificationManager {

    companion object {
        private const val TAG = "AndroidNotificationManager"
        private const val CHANNEL_ID = "diary_reminder"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE = 1002

        // Note: This will need to reference the androidApp module's NotificationReceiver
        // The receiver class name must match: net.chasmine.oneline.util.NotificationReceiver
        private const val NOTIFICATION_RECEIVER_CLASS = "net.chasmine.oneline.util.NotificationReceiver"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private fun createNotificationChannel() {
        val name = "日記リマインダー"
        val descriptionText = "日記を書くリマインダー通知"
        val importance = AndroidSystemNotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
            enableLights(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as AndroidSystemNotificationManager
        notificationManager.createNotificationChannel(channel)

        Log.d(TAG, "通知チャンネルを作成しました: $CHANNEL_ID")
    }

    override suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "日次通知をスケジュール: ${hour}:${minute}")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Android 12以降のアラーム権限チェック
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "正確なアラームの権限がありません - 不正確なアラームを使用します")
                    return@withContext scheduleInexactAlarm(hour, minute)
                }
            }

            val intent = createReceiverIntent()
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

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            Log.d(TAG, "アラームを正常にスケジュールしました")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "アラームのスケジュールに失敗しました", e)
            Result.failure(e)
        }
    }

    /**
     * Schedule inexact alarm (fallback for when exact alarms are not allowed)
     */
    private fun scheduleInexactAlarm(hour: Int, minute: Int): Result<Unit> {
        return try {
            Log.d(TAG, "不正確なアラームを使用します: ${hour}:${minute}")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = createReceiverIntent()
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

            Log.d(TAG, "不正確なアラームを正常にスケジュールしました")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "不正確なアラームのスケジュールに失敗しました", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelDailyNotification(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "日次通知をキャンセルします")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = createReceiverIntent()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "アラームをキャンセルしました")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "アラームのキャンセルに失敗しました", e)
            Result.failure(e)
        }
    }

    override suspend fun showNotification(title: String, body: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "通知を表示します: title=$title")

            // 通知権限チェック
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "通知権限がありません")
                    return@withContext Result.failure(
                        SecurityException("POST_NOTIFICATIONS permission not granted")
                    )
                }
            }

            // MainActivity への Intent（通知タップ時に開く）
            val activityIntent = createMainActivityIntent()
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 通知アイコンのリソースID（androidApp モジュールのR.drawable参照）
            // Note: shared モジュールから androidApp の R クラスは参照できないため、
            // リソースIDを動的に取得する必要があります
            val iconResId = getNotificationIconResId()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, notification)
            }

            Log.d(TAG, "通知を正常に表示しました")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "通知の表示に失敗しました", e)
            Result.failure(e)
        }
    }

    override suspend fun requestPermission(): Result<Boolean> = withContext(Dispatchers.Main) {
        try {
            // Android 13未満では権限リクエスト不要
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@withContext Result.success(true)
            }

            // Note: 実際の権限リクエストはActivityで行う必要があるため、
            // ここでは現在の権限状態のみを返す
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "通知権限の状態: $hasPermission")
            Result.success(hasPermission)

        } catch (e: Exception) {
            Log.e(TAG, "権限チェックに失敗しました", e)
            Result.failure(e)
        }
    }

    override suspend fun hasPermission(): Boolean = withContext(Dispatchers.IO) {
        // Android 13未満では権限チェック不要
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@withContext true
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "通知権限の確認: $hasPermission")
        hasPermission
    }

    override suspend fun canScheduleExactAlarms(): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Android 12未満では常にtrue
            return@withContext true
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canSchedule = alarmManager.canScheduleExactAlarms()

        Log.d(TAG, "正確なアラームのスケジュール可否: $canSchedule")
        canSchedule
    }

    /**
     * Create Intent for NotificationReceiver
     */
    private fun createReceiverIntent(): Intent {
        return Intent().apply {
            // NotificationReceiverのクラス名を指定
            setClassName(context, NOTIFICATION_RECEIVER_CLASS)
        }
    }

    /**
     * Create Intent for MainActivity
     */
    private fun createMainActivityIntent(): Intent {
        return Intent().apply {
            setClassName(context, "net.chasmine.oneline.ui.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    /**
     * Get notification icon resource ID
     *
     * Since shared module cannot access androidApp's R class directly,
     * we need to get the resource ID dynamically
     */
    private fun getNotificationIconResId(): Int {
        return try {
            // Try to get ic_stat_name from resources
            context.resources.getIdentifier(
                "ic_stat_name",
                "drawable",
                context.packageName
            ).let { resId ->
                if (resId != 0) resId
                else android.R.drawable.ic_dialog_info // Fallback icon
            }
        } catch (e: Exception) {
            Log.w(TAG, "通知アイコンの取得に失敗しました。デフォルトアイコンを使用します", e)
            android.R.drawable.ic_dialog_info
        }
    }
}

/**
 * Actual implementation of createNotificationManager for Android
 *
 * Note: Requires Context, which should be provided through dependency injection (Koin)
 */
actual fun createNotificationManager(): NotificationManager {
    // This function cannot be used without DI context
    // Use AndroidNotificationManager(context) directly or inject through Koin
    throw UnsupportedOperationException(
        "createNotificationManager() requires Android Context. " +
        "Use AndroidNotificationManager(context) directly or inject through Koin."
    )
}
