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
    var showPermissionDeniedInfo by remember { mutableStateOf(false) }
    
    // 権限状態をチェック
    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    
    // 権限が拒否されている場合の表示判定
    LaunchedEffect(isNotificationEnabled, hasPermission) {
        showPermissionDeniedInfo = isNotificationEnabled && !hasPermission && 
                notificationPrefs.isPermissionRequested()
    }
    
    // 通知権限のリクエスト
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPrefs.setPermissionRequested()
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
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp), // カード内の余白を増加
            verticalArrangement = Arrangement.spacedBy(20.dp) // 要素間の余白を増加
        ) {
            // 権限拒否時の案内
            if (showPermissionDeniedInfo) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️ 通知権限が必要です",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "日記リマインダーを受け取るには、端末の設定から通知権限を有効にしてください。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = {
                                // 設定アプリを開く
                                val intent = android.content.Intent().apply {
                                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("設定アプリを開く")
                        }
                    }
                }
            }
            
            // 通知ON/OFF設定
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp) // Switchとの間に適切な余白を確保
                ) {
                    Text(
                        text = "日記リマインダー",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "毎日決まった時間に日記を書くリマインダーを受け取る",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
                
                // Switchを上部に配置し、テキストとの干渉を防ぐ
                Switch(
                    checked = isNotificationEnabled,
                    modifier = Modifier.padding(top = 4.dp), // タイトルと高さを合わせる
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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
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
                        .padding(vertical = 12.dp), // より適切な縦方向の余白
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp) // 時刻表示との間に余白を確保
                    ) {
                        Text(
                            text = "通知時刻",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "タップして時刻を変更",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // 時刻表示を見やすくする
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = String.format("%02d:%02d", notificationHour, notificationMinute),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            
            // テスト通知ボタン（デバッグ用）
            if (isNotificationEnabled) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                Button(
                    onClick = {
                        notificationManager.showTestNotification()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("🧪 テスト通知を送信")
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
