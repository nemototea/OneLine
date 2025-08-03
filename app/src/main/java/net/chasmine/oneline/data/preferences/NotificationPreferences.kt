package net.chasmine.oneline.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationPreferences private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: NotificationPreferences? = null
        
        fun getInstance(context: Context): NotificationPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_NOTIFICATION_HOUR = "notification_hour"
        private const val KEY_NOTIFICATION_MINUTE = "notification_minute"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // デフォルト値を変更：初回はON、時刻は18:00
    private val _isNotificationEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true))
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled
    
    private val _notificationHour = MutableStateFlow(prefs.getInt(KEY_NOTIFICATION_HOUR, 18))
    val notificationHour: StateFlow<Int> = _notificationHour
    
    private val _notificationMinute = MutableStateFlow(prefs.getInt(KEY_NOTIFICATION_MINUTE, 0))
    val notificationMinute: StateFlow<Int> = _notificationMinute
    
    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
        _isNotificationEnabled.value = enabled
    }
    
    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_NOTIFICATION_HOUR, hour)
            .putInt(KEY_NOTIFICATION_MINUTE, minute)
            .apply()
        _notificationHour.value = hour
        _notificationMinute.value = minute
    }
    
    // 初回起動チェック
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    // 権限リクエスト履歴
    fun isPermissionRequested(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
    }
    
    fun setPermissionRequested() {
        prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
    }
}
