package net.chasmine.oneline.util

import android.content.Context

/**
 * Android implementation of NotificationManager
 *
 * This is a stub implementation for Phase 6-1 (abstraction).
 * Full implementation will be provided in Phase 6-2.
 */
class AndroidNotificationManager(private val context: Context) : NotificationManager {
    
    override suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit> {
        // Stub implementation - will be implemented in Phase 6-2
        return Result.success(Unit)
    }

    override suspend fun cancelDailyNotification(): Result<Unit> {
        // Stub implementation - will be implemented in Phase 6-2
        return Result.success(Unit)
    }

    override suspend fun showNotification(title: String, body: String): Result<Unit> {
        // Stub implementation - will be implemented in Phase 6-2
        return Result.success(Unit)
    }

    override suspend fun requestPermission(): Result<Boolean> {
        // Stub implementation - will be implemented in Phase 6-2
        return Result.success(true)
    }

    override suspend fun hasPermission(): Boolean {
        // Stub implementation - will be implemented in Phase 6-2
        return true
    }

    override suspend fun canScheduleExactAlarms(): Boolean {
        // Stub implementation - will be implemented in Phase 6-2
        return true
    }
}

/**
 * Actual implementation of createNotificationManager for Android
 *
 * Note: Requires Context, which should be provided through dependency injection
 */
actual fun createNotificationManager(): NotificationManager {
    // This will be properly implemented in Phase 6-2
    // For now, throw an exception to indicate this needs Context
    throw UnsupportedOperationException(
        "createNotificationManager() requires Android Context. " +
        "Use AndroidNotificationManager(context) directly or inject through DI."
    )
}
