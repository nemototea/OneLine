package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreenImpl(
    onNavigateBack: () -> Unit,
    onNavigateToDataStorage: () -> Unit,
    onNavigateToGitSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToKmpVerification: () -> Unit = {},
    settingsManager: SettingsManager
) {
    val currentThemeMode by settingsManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDeveloperMode by settingsManager.isDeveloperMode.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "設定",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // DESIGN.md: 設定はデータ保存・通知・テーマ・情報の4群に整理する
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "データ保存") {
                    SettingsItem(
                        icon = Icons.Outlined.Storage,
                        title = "データ保存設定",
                        subtitle = "ローカル保存またはGit連携",
                        onClick = onNavigateToDataStorage
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Cloud,
                        title = "Git連携設定",
                        subtitle = "GitHubリポジトリとの同期設定",
                        onClick = onNavigateToGitSettings
                    )
                }
            }

            item {
                SettingsSection(title = "通知") {
                    SettingsItem(
                        icon = Icons.Outlined.Notifications,
                        title = "通知設定",
                        subtitle = "日記リマインダーの設定",
                        onClick = onNavigateToNotificationSettings
                    )
                }
            }

            item {
                SettingsSection(title = "テーマ") {
                    SettingsItem(
                        icon = Icons.Outlined.Palette,
                        title = "テーマ",
                        subtitle = currentThemeMode.displayName,
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "情報") {
                    SettingsItem(
                        icon = Icons.Outlined.Info,
                        title = "アプリについて",
                        subtitle = "バージョン情報・ライセンス",
                        onClick = onNavigateToAbout
                    )
                }
            }

            // 開発者モードが有効な場合のみ表示
            if (isDeveloperMode) {
                item {
                    SettingsSection(title = "開発者向け") {
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = "KMP/CMP 動作確認",
                            subtitle = "マルチプラットフォーム機能のテスト",
                            onClick = onNavigateToKmpVerification
                        )
                    }
                }
            }
        }
    }

    // テーマ選択ダイアログ
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("テーマを選択") },
            text = {
                Column {
                    ThemeMode.values().forEach { themeMode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        settingsManager.setThemeMode(themeMode)
                                        showThemeDialog = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentThemeMode == themeMode,
                                onClick = {
                                    scope.launch {
                                        settingsManager.setThemeMode(themeMode)
                                        showThemeDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = themeMode.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        // 紙のカード: surface ＋ 折り目線。影と色面で区切らない
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            // 補助的な導線なので焙じ茶（secondary）。朱は主アクションに取っておく
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "進む",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
