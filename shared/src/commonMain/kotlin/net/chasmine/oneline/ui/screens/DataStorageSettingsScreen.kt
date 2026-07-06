package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.repository.MigrationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageSettingsScreenImpl(
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
                title = {
                    Text(
                        text = "データ保存設定",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 現在の設定表示（濃い和紙の帯で、いま選ばれている状態を静かに示す）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isLocalOnlyMode) Icons.Outlined.PhoneAndroid else Icons.Outlined.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )

                    Column {
                        Text(
                            text = if (isLocalOnlyMode) "ローカル保存のみ" else "Git連携",
                            style = MaterialTheme.typography.titleMedium
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
            StorageOptionCard(
                selected = isLocalOnlyMode,
                title = "ローカル保存のみ",
                description = "• 設定不要ですぐ使える\n• 完全プライベート\n• 端末紛失でデータ消失のリスク",
                onClick = {
                    if (!isLocalOnlyMode) {
                        showMigrationDialog = true
                    }
                }
            )

            // Git連携オプション
            StorageOptionCard(
                selected = !isLocalOnlyMode,
                title = "Git連携",
                description = "• 自動バックアップ\n• 複数端末で同期\n• GitHubの設定が必要",
                onClick = {
                    if (isLocalOnlyMode) {
                        onNavigateToGitSettings()
                    }
                }
            ) {
                if (!isLocalOnlyMode && gitRepoUrl.isBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Git設定が必要です",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Git設定ボタン
            if (!isLocalOnlyMode) {
                Button(
                    onClick = onNavigateToGitSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
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
            containerColor = MaterialTheme.colorScheme.surface,
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

/**
 * データ保存モードの選択カード（DESIGN.md: カード地の使い分け）
 *
 * 選択中は朱の淡い滲み（primaryContainer）、非選択は紙＋折り目線。
 * これがこの画面で primaryContainer を使う唯一の「本物の選択状態」。
 */
@Composable
private fun StorageOptionCard(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
    extraContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                    selected = selected,
                    onClick = onClick
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            extraContent?.invoke()
        }
    }
}
