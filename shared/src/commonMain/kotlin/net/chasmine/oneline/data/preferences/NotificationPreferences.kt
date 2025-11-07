package net.chasmine.oneline.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.chasmine.oneline.util.synchronized
import kotlin.concurrent.Volatile

/**
 * 通知設定の管理クラス（共通コード）
 *
 * SettingsStorageを使用して通知設定を保存・取得します。
 * シングルトンパターンを採用し、アプリ全体で一つのインスタンスを共有します。
 */
class NotificationPreferences(private val storage: SettingsStorage) {

    companion object {
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_NOTIFICATION_HOUR = "notification_hour"
        private const val KEY_NOTIFICATION_MINUTE = "notification_minute"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested"

        // デフォルト値
        private const val DEFAULT_NOTIFICATION_ENABLED = true
        private const val DEFAULT_NOTIFICATION_HOUR = 18
        private const val DEFAULT_NOTIFICATION_MINUTE = 0

        @Volatile
        private var INSTANCE: NotificationPreferences? = null

        fun getInstance(storage: SettingsStorage): NotificationPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationPreferences(storage).also {
                    INSTANCE = it
                }
            }
        }

        // 明示的なクリーンアップメソッド
        fun destroyInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }

    /**
     * 通知が有効かどうか
     */
    val isNotificationEnabled: Flow<Boolean> = storage.observeBoolean(KEY_NOTIFICATION_ENABLED)
        .map { it || DEFAULT_NOTIFICATION_ENABLED }

    /**
     * 通知時刻（時）
     */
    val notificationHour: Flow<Int> = storage.observeString(KEY_NOTIFICATION_HOUR)
        .map { it?.toIntOrNull() ?: DEFAULT_NOTIFICATION_HOUR }

    /**
     * 通知時刻（分）
     */
    val notificationMinute: Flow<Int> = storage.observeString(KEY_NOTIFICATION_MINUTE)
        .map { it?.toIntOrNull() ?: DEFAULT_NOTIFICATION_MINUTE }

    /**
     * 通知の有効/無効を設定
     */
    suspend fun setNotificationEnabled(enabled: Boolean) {
        storage.saveBoolean(KEY_NOTIFICATION_ENABLED, enabled)
    }

    /**
     * 通知時刻を設定
     */
    suspend fun setNotificationTime(hour: Int, minute: Int) {
        storage.saveString(KEY_NOTIFICATION_HOUR, hour.toString())
        storage.saveString(KEY_NOTIFICATION_MINUTE, minute.toString())
    }

    /**
     * 初回起動かどうかを確認
     */
    suspend fun isFirstLaunch(): Boolean {
        return storage.getString(KEY_FIRST_LAUNCH)?.toBooleanStrictOrNull() ?: true
    }

    /**
     * 初回起動フラグをクリア
     */
    suspend fun setFirstLaunchCompleted() {
        storage.saveString(KEY_FIRST_LAUNCH, "false")
    }

    /**
     * 権限リクエストが既に行われたかどうかを確認
     */
    suspend fun isPermissionRequested(): Boolean {
        return storage.getString(KEY_PERMISSION_REQUESTED)?.toBooleanStrictOrNull() ?: false
    }

    /**
     * 権限リクエストを記録
     */
    suspend fun setPermissionRequested() {
        storage.saveString(KEY_PERMISSION_REQUESTED, "true")
    }
}
