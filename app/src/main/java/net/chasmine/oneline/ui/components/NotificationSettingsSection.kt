package net.chasmine.oneline.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import net.chasmine.oneline.data.preferences.NotificationPreferences
import net.chasmine.oneline.util.DiaryNotificationManager

@Composable
fun NotificationSettingsSection() {
    val context = LocalContext.current
    val notificationPrefs = remember { NotificationPreferences.getInstance(context) }
    val notificationManager = remember { DiaryNotificationManager(context) }
    
    val isNotificationEnabled by notificationPrefs.isNotificationEnabled.collectAsState()
    val notificationHour by notificationPrefs.notificationHour.collectAsState()
    val notificationMinute by notificationPrefs.notificationMinute.collectAsState()
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // 通知権限のリクエスト
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationPrefs.setNotificationEnabled(true)
            notificationManager.scheduleDaily(notificationHour, notificationMinute)
        } else {
            showPermissionDialog = true
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 通知ON/OFF設定
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "日記リマインダー",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "毎日決まった時間に日記を書くリマインダーを受け取る",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isNotificationEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // 通知権限をチェック
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                when (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )) {
                                    PackageManager.PERMISSION_GRANTED -> {
                                        notificationPrefs.setNotificationEnabled(true)
                                        notificationManager.scheduleDaily(notificationHour, notificationMinute)
                                    }
                                    else -> {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            } else {
                                notificationPrefs.setNotificationEnabled(true)
                                notificationManager.scheduleDaily(notificationHour, notificationMinute)
                            }
                        } else {
                            notificationPrefs.setNotificationEnabled(false)
                            notificationManager.cancelDaily()
                        }
                    }
                )
            }
            
            // 時間設定
            if (isNotificationEnabled) {
                Divider()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    notificationPrefs.setNotificationTime(hour, minute)
                                    notificationManager.cancelDaily()
                                    notificationManager.scheduleDaily(hour, minute)
                                },
                                notificationHour,
                                notificationMinute,
                                true
                            ).show()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "通知時刻",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "タップして時刻を変更",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format("%02d:%02d", notificationHour, notificationMinute),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // 権限拒否時のダイアログ
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("通知権限が必要です") },
            text = { 
                Text("日記リマインダーを受け取るには、通知権限を許可してください。設定アプリから権限を有効にできます。") 
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
