package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.repository.MigrationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGitSettings: () -> Unit,
    settingsManager: SettingsManager,
    onMigrateToLocalMode: suspend (clearGitData: Boolean) -> MigrationResult
) {
    val scope = rememberCoroutineScope()

    val isLocalOnlyMode by settingsManager.isLocalOnlyMode.collectAsState(initial = false)
    val gitRepoUrl by settingsManager.gitRepoUrl.collectAsState(initial = "")
    val gitUsername by settingsManager.gitUsername.collectAsState(initial = "")

    var showMigrationDialog by remember { mutableStateOf(false) }
    var migrationInProgress by remember { mutableStateOf(false) }
    var migrationResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("データ保存設定") },
                windowInsets = WindowInsets(0, 0, 0, 0),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 現在の設定表示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isLocalOnlyMode) Icons.Default.Phone else Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column {
                        Text(
                            text = if (isLocalOnlyMode) "📱 ローカル保存のみ" else "☁️ Git連携",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isLocalOnlyMode) {
                                "端末内にのみ保存"
                            } else {
                                gitRepoUrl.takeIf { it.isNotBlank() }?.let {
                                    // URLから簡潔な表示を生成
                                    it.substringAfterLast("/").removeSuffix(".git")
                                } ?: "未設定"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ローカル保存オプション
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isLocalOnlyMode) {
                            showMigrationDialog = true
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocalOnlyMode)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = isLocalOnlyMode,
                            onClick = {
                                if (!isLocalOnlyMode) {
                                    showMigrationDialog = true
                                }
                            }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ローカル保存のみ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• 設定不要ですぐ使える\n• 完全プライベート\n• 端末紛失でデータ消失のリスク",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Git連携オプション
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isLocalOnlyMode) {
                            onNavigateToGitSettings()
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (!isLocalOnlyMode)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = !isLocalOnlyMode,
                            onClick = {
                                if (isLocalOnlyMode) {
                                    onNavigateToGitSettings()
                                }
                            }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Git連携",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• 自動バックアップ\n• 複数端末で同期\n• GitHubの設定が必要",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isLocalOnlyMode && gitRepoUrl.isBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Git設定が必要です",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Git設定ボタン
            if (!isLocalOnlyMode) {
                Button(
                    onClick = onNavigateToGitSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Git設定を開く")
                }
            }
        }
    }

    // 移行確認ダイアログ
    if (showMigrationDialog) {
        AlertDialog(
            onDismissRequest = { showMigrationDialog = false },
            title = { Text("ローカル保存に切り替え") },
            text = {
                Column {
                    Text("Git連携からローカル保存のみに切り替えますか？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 既存のデータはローカルにコピーされます\n• Git設定は削除されます\n• 今後は端末内にのみ保存されます",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMigrationDialog = false
                        migrationInProgress = true
                        scope.launch {
                            try {
                                val result = onMigrateToLocalMode(true)
                                migrationResult = when (result) {
                                    is MigrationResult.Success -> "ローカル保存に切り替えました"
                                    else -> "切り替えに失敗しました"
                                }
                            } catch (e: Exception) {
                                migrationResult = "エラーが発生しました: ${e.message}"
                            } finally {
                                migrationInProgress = false
                            }
                        }
                    },
                    enabled = !migrationInProgress
                ) {
                    Text("切り替える")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMigrationDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 移行中ダイアログ
    if (migrationInProgress) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("切り替え中...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("データを移行しています")
                }
            },
            confirmButton = { }
        )
    }

    // 移行結果ダイアログ
    migrationResult?.let { result ->
        AlertDialog(
            onDismissRequest = { migrationResult = null },
            title = { Text("切り替え完了") },
            text = { Text(result) },
            confirmButton = {
                TextButton(onClick = { migrationResult = null }) {
                    Text("OK")
                }
            }
        )
    }
}
