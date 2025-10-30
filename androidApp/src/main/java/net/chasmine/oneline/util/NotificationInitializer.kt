package net.chasmine.oneline.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import net.chasmine.oneline.data.preferences.NotificationPreferences

class NotificationInitializer(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationInitializer"
    }
    
    private val notificationPrefs = NotificationPreferences.getInstance(context)
    private val notificationManager = DiaryNotificationManager(context)
    
    /**
     * アプリ起動時の通知初期化処理
     * 各ユースケースに応じて適切な処理を実行
     */
    suspend fun initializeOnAppStart(): InitializationResult {
        Log.d(TAG, "アプリ起動時の通知初期化を開始")
        
        val isFirstLaunch = notificationPrefs.isFirstLaunch()
        val isNotificationEnabled = notificationPrefs.isNotificationEnabled.first()
        val hasNotificationPermission = hasNotificationPermission()
        
        Log.d(TAG, "初回起動: $isFirstLaunch, 通知有効: $isNotificationEnabled, 権限: $hasNotificationPermission")
        
        return when {
            // ケース1: 初回起動
            isFirstLaunch -> {
                Log.d(TAG, "初回起動時の処理を実行")
                handleFirstLaunch()
            }
            
            // ケース2: 通知ON + 権限あり
            isNotificationEnabled && hasNotificationPermission -> {
                Log.d(TAG, "通知有効 + 権限ありの処理を実行")
                handleNotificationEnabledWithPermission()
            }
            
            // ケース3: 通知ON + 権限なし
            isNotificationEnabled && !hasNotificationPermission -> {
                Log.d(TAG, "通知有効 + 権限なしの処理を実行")
                handleNotificationEnabledWithoutPermission()
            }
            
            // ケース4: 通知OFF
            !isNotificationEnabled -> {
                Log.d(TAG, "通知無効の処理を実行")
                handleNotificationDisabled()
            }
            
            else -> {
                Log.w(TAG, "予期しないケース")
                InitializationResult.NoAction
            }
        }
    }
    
    /**
     * 初回起動時の処理
     */
    private suspend fun handleFirstLaunch(): InitializationResult {
        Log.d(TAG, "初回起動: デフォルト設定を適用")
        
        // デフォルト設定は既にNotificationPreferencesで設定済み（ON, 18:00）
        notificationPrefs.setFirstLaunchCompleted()
        
        return if (hasNotificationPermission()) {
            // 既に権限がある場合（Android 12以前など）
            Log.d(TAG, "初回起動: 権限あり、通知をスケジュール")
            scheduleNotification()
            InitializationResult.ScheduledWithPermission
        } else {
            // 権限が必要な場合
            Log.d(TAG, "初回起動: 権限リクエストが必要")
            InitializationResult.PermissionRequired
        }
    }
    
    /**
     * 通知ON + 権限ありの処理
     */
    private suspend fun handleNotificationEnabledWithPermission(): InitializationResult {
        Log.d(TAG, "通知有効 + 権限あり: スケジュールを更新")
        scheduleNotification()
        return InitializationResult.ScheduledWithPermission
    }
    
    /**
     * 通知ON + 権限なしの処理
     */
    private suspend fun handleNotificationEnabledWithoutPermission(): InitializationResult {
        Log.d(TAG, "通知有効 + 権限なし: 権限リクエストが必要")
        
        val wasPermissionRequested = notificationPrefs.isPermissionRequested()
        return if (wasPermissionRequested) {
            // 以前に権限をリクエストしたが拒否された
            InitializationResult.PermissionPreviouslyDenied
        } else {
            // まだ権限をリクエストしていない
            InitializationResult.PermissionRequired
        }
    }
    
    /**
     * 通知OFFの処理
     */
    private suspend fun handleNotificationDisabled(): InitializationResult {
        Log.d(TAG, "通知無効: スケジュールをキャンセル")
        notificationManager.cancelDaily()
        return InitializationResult.Cancelled
    }
    
    /**
     * 通知をスケジュール
     */
    private suspend fun scheduleNotification() {
        val hour = notificationPrefs.notificationHour.first()
        val minute = notificationPrefs.notificationMinute.first()
        Log.d(TAG, "通知をスケジュール: ${hour}:${minute}")
        notificationManager.scheduleDaily(hour, minute)
    }
    
    /**
     * 通知権限の確認
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12以前は権限不要
            true
        }
    }
    
    /**
     * 権限リクエスト後の処理
     */
    suspend fun handlePermissionResult(granted: Boolean) {
        Log.d(TAG, "権限リクエスト結果: $granted")
        
        notificationPrefs.setPermissionRequested()
        
        if (granted) {
            Log.d(TAG, "権限許可: 通知をスケジュール")
            scheduleNotification()
        } else {
            Log.d(TAG, "権限拒否: 通知を無効化")
            notificationPrefs.setNotificationEnabled(false)
        }
    }
    
    /**
     * 初期化結果
     */
    sealed class InitializationResult {
        object ScheduledWithPermission : InitializationResult()
        object PermissionRequired : InitializationResult()
        object PermissionPreviouslyDenied : InitializationResult()
        object Cancelled : InitializationResult()
        object NoAction : InitializationResult()
    }
}
