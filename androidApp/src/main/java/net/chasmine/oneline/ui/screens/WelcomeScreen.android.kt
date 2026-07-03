package net.chasmine.oneline.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.preferences.NotificationPreferences
import net.chasmine.oneline.data.preferences.SettingsManagerFactory
import net.chasmine.oneline.util.AndroidNotificationManager

/**
 * WelcomeScreen wrapper for Android
 *
 * 共有実装（WelcomeScreenImpl）に、Android固有の通知権限リクエスト・
 * 時刻ピッカー・通知スケジューリングを注入する。
 */
@Composable
fun WelcomeScreen(
    onStartFirstEntry: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManagerFactory.getInstance(context) }
    val notificationPrefs = remember { NotificationPreferences.getInstance(context) }
    val notificationManager = remember { AndroidNotificationManager(context) }
    val scope = rememberCoroutineScope()

    val notificationEnabled by notificationPrefs.isNotificationEnabled.collectAsState()
    val notificationHour by notificationPrefs.notificationHour.collectAsState()
    val notificationMinute by notificationPrefs.notificationMinute.collectAsState()

    // 通知権限のリクエスト（Android 13+）
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPrefs.setPermissionRequested()
        if (isGranted) {
            notificationPrefs.setNotificationEnabled(true)
            scope.launch {
                notificationManager.scheduleDailyNotification(notificationHour, notificationMinute)
            }
        } else {
            // 拒否されたらスイッチをOFFに戻す（設定画面から再度有効化できる）
            notificationPrefs.setNotificationEnabled(false)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    WelcomeScreenImpl(
        notificationEnabled = notificationEnabled,
        notificationHour = notificationHour,
        notificationMinute = notificationMinute,
        onNotificationToggle = { enabled ->
            if (enabled) {
                if (hasNotificationPermission()) {
                    notificationPrefs.setNotificationEnabled(true)
                    scope.launch {
                        notificationManager.scheduleDailyNotification(
                            notificationHour,
                            notificationMinute
                        )
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                notificationPrefs.setNotificationEnabled(false)
                scope.launch {
                    notificationManager.cancelDailyNotification()
                }
            }
        },
        onPickNotificationTime = {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    notificationPrefs.setNotificationTime(hour, minute)
                    scope.launch {
                        notificationManager.cancelDailyNotification()
                        notificationManager.scheduleDailyNotification(hour, minute)
                    }
                },
                notificationHour,
                notificationMinute,
                true
            ).show()
        },
        onStartFirstEntry = onStartFirstEntry,
        settingsManager = settingsManager
    )
}
