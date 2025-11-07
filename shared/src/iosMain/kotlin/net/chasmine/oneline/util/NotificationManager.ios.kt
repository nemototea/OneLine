package net.chasmine.oneline.util

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSLog
import platform.UserNotifications.*
import kotlin.coroutines.resume

/**
 * iOS implementation of NotificationManager
 *
 * Uses UNUserNotificationCenter for notification scheduling
 */
class IOSNotificationManager : NotificationManager {

    companion object {
        private const val TAG = "IOSNotificationManager"
        private const val NOTIFICATION_ID = "diary_reminder"
        private const val CATEGORY_ID = "diary_category"
    }

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun scheduleDailyNotification(hour: Int, minute: Int): Result<Unit> {
        return try {
            NSLog("$TAG: 日次通知をスケジュール: $hour:$minute")

            // 通知コンテンツの作成
            val content = UNMutableNotificationContent().apply {
                setTitle("今日の一行を書きませんか？")
                setBody("今日はどんな一日でしたか？日記を書いて記録しましょう。")
                setSound(UNNotificationSound.defaultSound)
                setCategoryIdentifier(CATEGORY_ID)
            }

            // 毎日指定時刻に通知するトリガーの作成
            val dateComponents = NSDateComponents().apply {
                setHour(hour.toLong())
                setMinute(minute.toLong())
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = dateComponents,
                repeats = true
            )

            // 通知リクエストの作成
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = NOTIFICATION_ID,
                content = content,
                trigger = trigger
            )

            // 既存の通知をキャンセルしてから新しい通知を追加
            notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(NOTIFICATION_ID))

            // 通知を追加
            suspendCancellableCoroutine { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        NSLog("$TAG: 通知のスケジュールに失敗: ${error.localizedDescription}")
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        NSLog("$TAG: 通知を正常にスケジュールしました")
                        continuation.resume(Result.success(Unit))
                    }
                }
            }
        } catch (e: Exception) {
            NSLog("$TAG: 通知のスケジュールに失敗: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun cancelDailyNotification(): Result<Unit> {
        return try {
            NSLog("$TAG: 日次通知をキャンセルします")

            notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(NOTIFICATION_ID))

            NSLog("$TAG: 通知をキャンセルしました")
            Result.success(Unit)

        } catch (e: Exception) {
            NSLog("$TAG: 通知のキャンセルに失敗: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun showNotification(title: String, body: String): Result<Unit> {
        return try {
            NSLog("$TAG: 通知を表示します: title=$title")

            // 通知コンテンツの作成
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound)
                setCategoryIdentifier(CATEGORY_ID)
            }

            // 即座に通知するトリガー（1秒後）
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = 1.0,
                repeats = false
            )

            // 通知リクエストの作成
            // iOSではユニークなIDを生成するためにUUIDを使用
            val uuid = platform.Foundation.NSUUID().UUIDString
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "immediate_notification_$uuid",
                content = content,
                trigger = trigger
            )

            // 通知を追加
            suspendCancellableCoroutine { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        NSLog("$TAG: 通知の表示に失敗: ${error.localizedDescription}")
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        NSLog("$TAG: 通知を正常に表示しました")
                        continuation.resume(Result.success(Unit))
                    }
                }
            }
        } catch (e: Exception) {
            NSLog("$TAG: 通知の表示に失敗: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun requestPermission(): Result<Boolean> {
        return try {
            NSLog("$TAG: 通知権限をリクエストします")

            val options = UNAuthorizationOptionAlert or
                         UNAuthorizationOptionSound or
                         UNAuthorizationOptionBadge

            suspendCancellableCoroutine { continuation ->
                notificationCenter.requestAuthorizationWithOptions(options) { granted, error ->
                    if (error != null) {
                        NSLog("$TAG: 権限リクエストに失敗: ${error.localizedDescription}")
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        NSLog("$TAG: 権限リクエスト結果: $granted")
                        continuation.resume(Result.success(granted))
                    }
                }
            }
        } catch (e: Exception) {
            NSLog("$TAG: 権限リクエストに失敗: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun hasPermission(): Boolean {
        return try {
            suspendCancellableCoroutine { continuation ->
                notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                    val isAuthorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
                    NSLog("$TAG: 通知権限の確認: $isAuthorized")
                    continuation.resume(isAuthorized)
                }
            }
        } catch (e: Exception) {
            NSLog("$TAG: 権限確認に失敗: ${e.message}")
            false
        }
    }

    override suspend fun canScheduleExactAlarms(): Boolean {
        // iOSでは常に正確な時刻に通知をスケジュール可能
        return true
    }
}

/**
 * Actual implementation of createNotificationManager for iOS
 */
actual fun createNotificationManager(): NotificationManager {
    return IOSNotificationManager()
}
