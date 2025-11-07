package net.chasmine.oneline.util

/**
 * Notification Manager interface
 *
 * Platform-specific notification implementation
 * Android: AlarmManager + NotificationCompat
 * iOS: UNUserNotificationCenter
 */
interface NotificationManager {
    /**
     * Schedule daily notification
     *
     * @param hour Hour (0-23)
     * @param minute Minute (0-59)
     * @return Result.success if successful, Result.failure otherwise
     */
    suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit>

    /**
     * Cancel daily notification
     *
     * @return Result.success if successful, Result.failure otherwise
     */
    suspend fun cancelDailyNotification(): Result<Unit>

    /**
     * Show notification immediately
     *
     * @param title Notification title
     * @param body Notification body
     * @return Result.success if successful, Result.failure otherwise
     */
    suspend fun showNotification(title: String, body: String): Result<Unit>

    /**
     * Request notification permission
     *
     * @return true if permission granted, false otherwise
     */
    suspend fun requestPermission(): Result<Boolean>

    /**
     * Check if notification permission is granted
     *
     * @return true if permission granted, false otherwise
     */
    suspend fun hasPermission(): Boolean

    /**
     * Check if exact alarms can be scheduled (Android 12+)
     *
     * @return true if can schedule, false otherwise
     */
    suspend fun canScheduleExactAlarms(): Boolean
}

/**
 * Create platform-specific NotificationManager instance
 *
 * expect/actual pattern for platform-specific implementation
 */
expect fun createNotificationManager(): NotificationManager
